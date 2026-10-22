package gov.cdc.izgateway.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.KeyStore;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.test.util.ReflectionTestUtils;

import gov.cdc.izgateway.security.crypto.CryptoSupport;

/**
 * Regression coverage for IGDD-3582: {@link TrustManagerProvider} must not freeze the trust
 * manager it hands out at startup -- a later keystore/truststore rotation (pushed via
 * {@link ServerSslBundleReloader}) must be reflected here too, via the
 * {@link SslBundles#addBundleUpdateHandler} registration in
 * {@link TrustManagerProvider#initializeTrustManager()}.
 */
@ExtendWith(MockitoExtension.class)
class TrustManagerProviderTests {

	private static final String BUNDLE_NAME = "izgw-server";

	@BeforeEach
	void registerBcFipsProviders() {
		CryptoSupport.initialize();
	}

	private static KeyStore emptyKeyStore() throws Exception {
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, null);
		return ks;
	}

	private static SslBundle bundleWithTrustStore(KeyStore trustStore) {
		return SslBundle.of(SslStoreBundle.of(null, null, trustStore));
	}

	private TrustManagerProvider newBundleBackedProvider(SslBundles sslBundles) {
		TrustManagerProvider provider = new TrustManagerProvider(sslBundles);
		ReflectionTestUtils.setField(provider, "sslBundleName", BUNDLE_NAME);
		ReflectionTestUtils.invokeMethod(provider, "initializeTrustManager");
		return provider;
	}

	@Test
	void loadsTrustManagerFromBundleOnInit() throws Exception {
		SslBundles sslBundles = mock(SslBundles.class);
		KeyStore initial = emptyKeyStore();
		when(sslBundles.getBundle(BUNDLE_NAME)).thenReturn(bundleWithTrustStore(initial));

		TrustManagerProvider provider = newBundleBackedProvider(sslBundles);

		assertSame(initial, provider.getTrustStore());
		assertNotNull(provider.getServerTrustManager());
	}

	@Test
	@SuppressWarnings("unchecked")
	void refreshesTrustManagerWhenBundleIsUpdated() throws Exception {
		SslBundles sslBundles = mock(SslBundles.class);
		KeyStore initial = emptyKeyStore();
		when(sslBundles.getBundle(BUNDLE_NAME)).thenReturn(bundleWithTrustStore(initial));

		TrustManagerProvider provider = newBundleBackedProvider(sslBundles);
		var initialTrustManager = provider.getServerTrustManager();

		ArgumentCaptor<Consumer<SslBundle>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
		verify(sslBundles).addBundleUpdateHandler(eq(BUNDLE_NAME), handlerCaptor.capture());

		KeyStore rotated = emptyKeyStore();
		handlerCaptor.getValue().accept(bundleWithTrustStore(rotated));

		assertSame(rotated, provider.getTrustStore());
		assertNotSame(initialTrustManager, provider.getServerTrustManager());
	}

	@Test
	void doesNotRegisterAnUpdateHandlerWhenNoBundleIsConfigured() {
		SslBundles sslBundles = mock(SslBundles.class);
		TrustManagerProvider provider = new TrustManagerProvider(sslBundles);
		ReflectionTestUtils.setField(provider, "sslBundleName", "");
		ReflectionTestUtils.setField(provider, "trustStorePath", "src/test/resources/does-not-exist.bcfks");
		ReflectionTestUtils.setField(provider, "trustStorePassword", "whatever");
		ReflectionTestUtils.setField(provider, "trustStoreType", "BCFKS");
		ReflectionTestUtils.setField(provider, "trustStoreProvider", "BCFIPS");

		org.junit.jupiter.api.Assertions.assertThrows(java.util.ServiceConfigurationError.class,
				() -> ReflectionTestUtils.invokeMethod(provider, "initializeTrustManager"));

		verify(sslBundles, never()).addBundleUpdateHandler(anyString(), any());
	}
}
