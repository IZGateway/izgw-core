package gov.cdc.izgateway.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.Test;

/**
 * Regression coverage for IGDD-3582: {@link CertificateValidator} takes a
 * {@code Supplier<X509TrustManager>} rather than a fixed instance, so it always validates against
 * whichever trust manager the supplier currently returns instead of one cached at construction.
 */
class CertificateValidatorTests {

	@Test
	void consultsWhicheverTrustManagerTheSupplierCurrentlyReturns() throws Exception {
		X509Certificate cert = mock(X509Certificate.class);
		X509TrustManager stale = mock(X509TrustManager.class);
		X509TrustManager current = mock(X509TrustManager.class);
		doThrow(new CertificateException("untrusted")).when(stale).checkClientTrusted(any(), anyString());

		X509TrustManager[] live = { stale };
		CertificateValidator validator = new CertificateValidator(() -> live[0]);

		assertFalse(validator.isValid(cert));
		verify(stale).checkClientTrusted(new X509Certificate[] { cert }, "TLS-client-auth");

		live[0] = current;
		assertTrue(validator.isValid(cert));
		verify(current).checkClientTrusted(new X509Certificate[] { cert }, "TLS-client-auth");
	}
}
