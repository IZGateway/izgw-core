package gov.cdc.izgateway.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.util.ServiceConfigurationError;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

/**
 * Regression coverage for IGDD-3582: {@link ServerSslBundleReloader} must detect a changed
 * keystore/truststore file by polling its modification time (not Spring Boot's inotify-based
 * {@code reload-on-update}, which does not work on the AWS EFS volume this application's
 * keystore actually lives on) and push a rebuilt bundle via {@link SslBundleRegistry#updateBundle}.
 * <p>
 * Keystore/truststore locations below are bare paths (no {@code file:} prefix), matching what
 * production actually configures via {@code SSL_SHARE}. A bare path resolves differently than a
 * {@code file:}-qualified one through a webapp-context {@link ResourceLoader}, so using
 * {@code file:} here would not exercise that resolution path.
 */
@ExtendWith(MockitoExtension.class)
class ServerSslBundleReloaderTests {

	private static final String BUNDLE_NAME = "izgw-server";

	@Mock
	private SslBundleRegistry sslBundleRegistry;

	@Mock
	private SslBundles sslBundles;

	@Mock
	private Environment environment;

	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	private ServerSslBundleReloader newReloader() {
		return new ServerSslBundleReloader(sslBundleRegistry, sslBundles, environment, resourceLoader);
	}

	private void stubBundleProperties(String keystorePath, String truststorePath) {
		when(environment.getProperty("server.ssl.bundle")).thenReturn(BUNDLE_NAME);
		when(environment.getProperty("spring.ssl.bundle.jks.izgw-server.keystore.location")).thenReturn(keystorePath);
		when(environment.getProperty("spring.ssl.bundle.jks.izgw-server.truststore.location")).thenReturn(truststorePath);
	}

	@Test
	void noOpsWhenBundleNameNotConfigured() {
		when(environment.getProperty("server.ssl.bundle")).thenReturn(null);

		ServerSslBundleReloader reloader = newReloader();
		reloader.primeLastSeenModified();
		reloader.reloadIfChanged();

		verify(sslBundles, never()).getBundle(anyString());
		verify(sslBundleRegistry, never()).updateBundle(anyString(), any());
	}

	@Test
	void noOpsWhenFileHasNotChangedSincePriming(@TempDir File dir) throws Exception {
		File keystoreFile = new File(dir, "awsdev_keystore.bcfks");
		Files.writeString(keystoreFile.toPath(), "initial");
		String path = keystoreFile.getAbsolutePath();
		stubBundleProperties(path, path);

		ServerSslBundleReloader reloader = newReloader();
		reloader.primeLastSeenModified();
		reloader.reloadIfChanged();

		verify(sslBundleRegistry, never()).updateBundle(anyString(), any());
	}

	@Test
	void reloadsAndPreservesKeyOptionsProtocolWhenFileChanges(@TempDir File dir) throws Exception {
		File keystoreFile = new File(dir, "awsdev_keystore.bcfks");
		Files.writeString(keystoreFile.toPath(), "initial");
		String path = keystoreFile.getAbsolutePath();
		stubBundleProperties(path, path);

		SslBundleKey key = SslBundleKey.of("key-password", "dev.izgateway.org");
		SslOptions options = SslOptions.of(new String[] { "TLS_AES_256_GCM_SHA384" }, new String[] { "TLSv1.3" });
		SslBundle current = SslBundle.of(null, key, options, "TLS");
		when(sslBundles.getBundle(BUNDLE_NAME)).thenReturn(current);

		ServerSslBundleReloader reloader = newReloader();
		reloader.primeLastSeenModified();

		// Simulate a rotation: a later mtime than what was primed.
		Thread.sleep(10);
		Files.writeString(keystoreFile.toPath(), "rotated");
		keystoreFile.setLastModified(System.currentTimeMillis() + 5000);

		reloader.reloadIfChanged();

		verify(sslBundleRegistry).updateBundle(org.mockito.ArgumentMatchers.eq(BUNDLE_NAME), org.mockito.ArgumentMatchers.argThat(
				updated -> updated.getKey() == key && updated.getOptions() == options
						&& "TLS".equals(updated.getProtocol())));
	}

	@Test
	void doesNotPropagateExceptionWhenBundleLookupFails(@TempDir File dir) throws Exception {
		File keystoreFile = new File(dir, "awsdev_keystore.bcfks");
		Files.writeString(keystoreFile.toPath(), "initial");
		String path = keystoreFile.getAbsolutePath();
		stubBundleProperties(path, path);
		when(sslBundles.getBundle(BUNDLE_NAME)).thenThrow(new RuntimeException("boom"));

		ServerSslBundleReloader reloader = newReloader();
		reloader.primeLastSeenModified();
		keystoreFile.setLastModified(System.currentTimeMillis() + 5000);

		reloader.reloadIfChanged();

		verify(sslBundleRegistry, never()).updateBundle(anyString(), any());
	}

	@Test
	void doesNotPropagateServiceConfigurationErrorWhenBundleUpdateFails(@TempDir File dir) throws Exception {
		File keystoreFile = new File(dir, "awsdev_keystore.bcfks");
		Files.writeString(keystoreFile.toPath(), "initial");
		String path = keystoreFile.getAbsolutePath();
		stubBundleProperties(path, path);

		SslBundle current = SslBundle.of(null, SslBundleKey.of("key-password", "dev.izgateway.org"),
				SslOptions.of(new String[] { "TLS_AES_256_GCM_SHA384" }, new String[] { "TLSv1.3" }), "TLS");
		when(sslBundles.getBundle(BUNDLE_NAME)).thenReturn(current);
		doThrow(new ServiceConfigurationError("boom")).when(sslBundleRegistry).updateBundle(anyString(), any());

		ServerSslBundleReloader reloader = newReloader();
		reloader.primeLastSeenModified();
		keystoreFile.setLastModified(System.currentTimeMillis() + 5000);

		reloader.reloadIfChanged();

		verify(sslBundleRegistry).updateBundle(anyString(), any());
	}
}
