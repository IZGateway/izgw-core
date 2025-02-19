package gov.cdc.izgateway.security;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * CertificateValidator provides utility methods for validating certificates.
 */
@Slf4j
public class CertificateValidator {
    private final X509TrustManager trustManager;

    public CertificateValidator(X509TrustManager trustManager) {
        this.trustManager = trustManager;
    }

    public boolean isValid(X509Certificate cert) {
        try {
            cert.checkValidity();
            trustManager.checkClientTrusted(new X509Certificate[]{cert}, "TLS-client-auth");
            return true;
        } catch (CertificateException e) {
            log.error("Certificate validation failed", e);
            return false;
        }
    }
}
