package gov.cdc.izgateway.security;

import gov.cdc.izgateway.logging.markers.Markers2;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.ServiceConfigurationError;

/**
 * This class provides the server trust manager for SSL connections.
 * It reads the trust store from the same {@code SslBundle} backing the embedded
 * HTTPS listener ({@code server.ssl.bundle}), so it always reflects whatever
 * material that bundle currently holds -- including after a hot reload.
 */
@Slf4j
@Component
public class TrustManagerProvider {

    private final SslBundles sslBundles;

    @Value("${server.ssl.bundle:}")
    private String sslBundleName;

    @Getter
    private X509TrustManager serverTrustManager;

    @Getter
    private KeyStore trustStore;

    public TrustManagerProvider(SslBundles sslBundles) {
        this.sslBundles = sslBundles;
    }

    @PostConstruct
    private void initializeTrustManager() {
        try {
            this.trustStore = sslBundles.getBundle(sslBundleName).getStores().getTrustStore();

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX", "BCJSSE");
            tmf.init(trustStore);

            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager t) {
                    this.serverTrustManager = t;
                    return;
                }
            }
        } catch (GeneralSecurityException e) {
            log.error(Markers2.append(e), "Cannot create Trust Manager from SSL bundle {}: {}", sslBundleName, e.getMessage());
            throw new ServiceConfigurationError(e.getMessage(), e);
        }
        throw new ServiceConfigurationError("No ServerTrustManager could be found.");
    }

    /**
     * Finds the issuer certificate for the given leaf certificate by matching its issuer DN
     * against the subject DN of each entry in the trust store.
     * Returns null if no match is found — callers should skip OCSP in that case.
     */
    public X509Certificate findIssuerCert(X509Certificate cert) {
        String issuerDN = cert.getIssuerX500Principal().getName();
        try {
            Enumeration<String> aliases = trustStore.aliases();
            while (aliases.hasMoreElements()) {
                Certificate candidate = trustStore.getCertificate(aliases.nextElement());
                if (candidate instanceof X509Certificate x509
                        && x509.getSubjectX500Principal().getName().equals(issuerDN)) {
                    return x509;
                }
            }
        } catch (KeyStoreException e) {
            log.warn("Error looking up issuer cert for {}", issuerDN, e);
        }
        return null;
    }
}