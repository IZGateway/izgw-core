package gov.cdc.izgateway.security;

import gov.cdc.izgateway.logging.markers.Markers2;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.ServiceConfigurationError;

/**
 * This class provides the server trust manager for SSL connections.
 * When {@code server.ssl.bundle} names a Spring Boot {@code SslBundle}, the trust store is
 * read from that bundle and kept current via {@link SslBundles#addBundleUpdateHandler}, so a
 * trust-store rotation (e.g. via {@link ServerSslBundleReloader}) is reflected here too, not
 * just in the embedded Tomcat connector. Otherwise, falls back to loading the legacy
 * {@code server.ssl.trust-store}/{@code -type}/{@code -provider} properties directly, once, for
 * consumers that have not migrated to a named bundle -- that path has no reload, by design (no
 * bundle means nothing to register an update handler against).
 */
@Slf4j
@Component
public class TrustManagerProvider {

    private final SslBundles sslBundles;

    @Value("${server.ssl.bundle:}")
    private String sslBundleName;

    @Value("${server.ssl.trust-store:}")
    private String trustStorePath;

    @Value("${server.ssl.trust-store-password:}")
    private String trustStorePassword;

    @Value("${server.ssl.trust-store-type:}")
    private String trustStoreType;

    @Value("${server.ssl.trust-store-provider:}")
    private String trustStoreProvider;

    @Getter
    private X509TrustManager serverTrustManager;

    @Getter
    private KeyStore trustStore;

    /**
     * Create the trust manager provider.
     * @param sslBundles The application's registered SSL bundles, used to resolve
     * {@code server.ssl.bundle} when a named bundle is configured.
     */
    public TrustManagerProvider(SslBundles sslBundles) {
        this.sslBundles = sslBundles;
    }

    @PostConstruct
    private void initializeTrustManager() {
        applyTrustStore(StringUtils.hasText(sslBundleName) ? loadFromBundle() : loadFromLegacyProperties(),
                StringUtils.hasText(sslBundleName) ? "SSL bundle " + sslBundleName : trustStorePath);
        if (StringUtils.hasText(sslBundleName)) {
            sslBundles.addBundleUpdateHandler(sslBundleName,
                    updatedBundle -> applyTrustStore(updatedBundle.getStores().getTrustStore(), "SSL bundle " + sslBundleName));
        }
    }

    /**
     * Builds a fresh trust manager from the given store and assigns both fields, or throws if
     * that fails. Called both from {@link #initializeTrustManager()} and from the bundle update
     * handler registered there, so a trust-store rotation refreshes {@link #serverTrustManager}
     * the same way the initial load does.
     */
    private void applyTrustStore(KeyStore newTrustStore, String source) {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX", "BCJSSE");
            tmf.init(newTrustStore);

            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager t) {
                    this.trustStore = newTrustStore;
                    this.serverTrustManager = t;
                    return;
                }
            }
        } catch (GeneralSecurityException e) {
            log.error(Markers2.append(e), "Cannot create Trust Manager from {}: {}", source, e.getMessage());
            throw new ServiceConfigurationError(e.getMessage(), e);
        }
        throw new ServiceConfigurationError("No ServerTrustManager could be found in " + source);
    }

    private KeyStore loadFromBundle() {
        try {
            return sslBundles.getBundle(sslBundleName).getStores().getTrustStore();
        } catch (NoSuchSslBundleException e) {
            log.error(Markers2.append(e), "Cannot create Trust Manager from SSL bundle {}: {}", sslBundleName, e.getMessage());
            throw new ServiceConfigurationError(e.getMessage(), e);
        }
    }

    private KeyStore loadFromLegacyProperties() {
        try {
            KeyStore ks = KeyStore.getInstance(trustStoreType, trustStoreProvider);
            try (FileInputStream fis = new FileInputStream(trustStorePath)) {
                ks.load(fis, trustStorePassword.toCharArray());
            }
            return ks;
        } catch (GeneralSecurityException | IOException e) {
            log.error(Markers2.append(e), "Cannot create Trust Manager from {}: {}", trustStorePath, e.getMessage());
            throw new ServiceConfigurationError(e.getMessage(), e);
        }
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