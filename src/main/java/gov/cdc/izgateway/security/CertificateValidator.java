package gov.cdc.izgateway.security;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.function.Supplier;

/**
 * CertificateValidator provides utility methods for validating certificates.
 * Takes a {@link Supplier} rather than a fixed {@link X509TrustManager}, so a caller backed by
 * {@link TrustManagerProvider} (whose trust manager can be replaced after a bundle reload, see
 * {@link TrustManagerProvider#initializeTrustManager()}) always validates against the current
 * trust manager instead of permanently caching whichever one existed at construction time.
 */
@Slf4j
public class CertificateValidator {
    private final Supplier<X509TrustManager> trustManager;

    public CertificateValidator(Supplier<X509TrustManager> trustManager) {
        this.trustManager = trustManager;
    }

    public boolean isValid(X509Certificate cert) {
        try {
            cert.checkValidity();
            trustManager.get().checkClientTrusted(new X509Certificate[]{cert}, "TLS-client-auth");
            return true;
        } catch (CertificateException e) {
            log.error("Certificate validation failed", e);
            return false;
        }
    }
}
