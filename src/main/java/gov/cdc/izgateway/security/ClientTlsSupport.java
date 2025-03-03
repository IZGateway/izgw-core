package gov.cdc.izgateway.security;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.coyote.http11.AbstractHttp11JsseProtocol;
import org.bouncycastle.jsse.util.URLConnectionUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.cdc.izgateway.logging.markers.Markers2;
import gov.cdc.izgateway.security.ocsp.RevocationTrustManager;
import gov.cdc.izgateway.utils.X500Utils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
/**
 * Configuration class for setting up SSL Communications with RESTful Endpoints.
 * This class loads the keystore from client.ssl.key-store using password client.ssl.key-password
 *
 */
public class ClientTlsSupport implements InitializingBean {
	private static final String BCJSSE = "BCJSSE";
    private static ScheduledExecutorService tse = 
    		Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Background-Scheduler-Trust"));

    public static class SslReloader {
    	private SslReloader() {}
    	private static AbstractHttp11JsseProtocol<?> protocol;
    	public static void setProtocol(AbstractHttp11JsseProtocol<?> protocol) {
    		SslReloader.protocol = protocol;
    	}
    	public static void reloadSsl() {
    		if (protocol != null) {
    			protocol.reloadSslHostConfigs();
    		}
    	}
    }
	@Getter
	private final ClientTlsConfiguration config;

	private KeyManagerFactory keyMgrFact;
	private SSLContext sslContext;
	
    @Getter
    private X509Certificate certificate;

	private Set<Runnable> trustChangedListeners = new LinkedHashSet<>();

	public ClientTlsSupport(@Autowired ClientTlsConfiguration config) {
		this.config = config;
	}

	public void afterPropertiesSet() throws IOException {
		/* Set JDK Default protocols and cipher suites for client and server connections. */
		String p = config.getProtocols().replace("+",",");
		System.setProperty("jdk.tls.client.protocols", p);
		System.setProperty("jdk.tls.server.protocols", p);
		System.setProperty("https.protocols", p);
		System.setProperty("jdk.tls.client.cipherSuites", config.getCipherSuites());
		System.setProperty("jdk.tls.server.cipherSuites", config.getCipherSuites());
		System.setProperty("https.cipherSuites", config.getCipherSuites());

		sslContext = getSSLContext();
		SSLContext.setDefault(sslContext);
		
		this.addSslTrustChangedListener(SslReloader::reloadSsl);  // Force server content update
		this.addSslTrustChangedListener(this::getSSLContext);
		
		tse.scheduleAtFixedRate(
			this::updateTrust, 					// Check for trust update needed
			config.getMonitoringPeriod(), 		// Initially wait for one monitoring period
			config.getMonitoringPeriod(), 		// But then monitoring at usual rate
			TimeUnit.SECONDS);					// specified in sections
	}

    public TrustManager[] getTrustManagers() {
        boolean reload = checkForUpdates();

        KeyStore trustStore = loadTrustStore(reload);
        TrustManager[] tm = { getTrustManager(trustStore) };

        return tm;
    }

	public SSLContext getSSLContext() {
		boolean reload = checkForUpdates();
		if (sslContext != null && !reload) {
			return sslContext;
		}

		try {
			KeyStore trustStore = loadTrustStore(reload);
			TrustManager[] tm = { getTrustManager(trustStore) };
			// Force to TSLv1.2 or higher
			sslContext = SSLContext.getInstance("TLSv1.2", BCJSSE);
			sslContext.init(getKeyManagerFactory(reload).getKeyManagers(), tm, null);
		}  catch (GeneralSecurityException e) {
			log.error(Markers2.append(e), "Cannot create SSL Context: {}", e.getMessage());
			throw new ServiceConfigurationError(e.getMessage(), e);
		}

		// Set the default SSL context to use for clients to enable MySQL Connector to use it.
		SSLContext.setDefault(sslContext);
		HttpsURLConnection.setDefaultHostnameVerifier((a,b) -> true);  // NOSONAR IZGW only uses certificate verification

		return sslContext;
	}

	KeyStore loadTrustStore(boolean reload) {
		return config.getClientTrustStore().load(reload);
	}
	
	/**
	 * Get the keystore used by the Client.  NOTE: This keystore is also the source of content for
	 * the trust store of the server.
	 *
	 * @return	The common key store.
	 * @throws KeyStoreException
	 * @throws CertificateException
	 */
	synchronized KeyStore loadKeyStore(boolean reload) {
		KeyStore ks = config.getClientKeyStore().load(reload);
		setClientHostname(ks);
		return ks;
	}

	private void setClientHostname(KeyStore keystore) {
		String cn = checkCertificate(keystore, config.getClientHostname());
		if (StringUtils.isEmpty(config.getClientHostname())) {
			config.setClientHostname(cn);
			log.info("Client Hostname set to {}", cn);
		}
	}

	private X509ExtendedTrustManager getTrustManager(KeyStore keystore) {
		TrustManagerFactory trustMgrFact;
		try {
			trustMgrFact = TrustManagerFactory.getInstance("PKIX", BCJSSE);
			trustMgrFact.init(keystore);
		} catch (GeneralSecurityException e) {
			log.error(Markers2.append(e), "Cannot create Trust Manager from {}: {}", e.getMessage());
			throw new ServiceConfigurationError(e.getMessage(), e);
		}
		return new RevocationTrustManager((X509ExtendedTrustManager) trustMgrFact.getTrustManagers()[0]);
	}

	/**
	 * Check the private key and certificate for the server.
	 * @param keystore The keystore to check
	 * @param commonName the Certificate Common Name to find a key for, or null if using the first one.
	 * @throws CertificateException
	 * @throws KeyStoreException
	 */
	private String checkCertificate(KeyStore keystore, String commonName) {
		try {
			Enumeration<String> e = keystore.aliases();
			while (e.hasMoreElements()) {
				String alias = e.nextElement();

				X509Certificate c = (X509Certificate) keystore.getCertificate(alias);
				if (StringUtils.isNotEmpty(commonName) && StringUtils.equals(X500Utils.getCommonName(c), commonName) ||
					StringUtils.isEmpty(commonName)
				) {
					String cn = X500Utils.getCommonName(c);
					if (!keystore.isKeyEntry(alias)) {
						if (StringUtils.isEmpty(commonName)) {
							continue;
						}
						log.error("No Private Key found for {}", cn);
						throw new ServiceConfigurationError("No Private Key found for " + cn);
					}
					
					log.info("Valid Certificate found for {}", cn);
                    certificate = c;
                    certificate.checkValidity();
					return cn;
				} 
			}
		} catch (KeyStoreException e1) {
			log.error(Markers2.append(e1), "Cannot enumerate certificates");
			throw new ServiceConfigurationError(e1.getMessage(), e1);
		} catch (CertificateExpiredException e1) {
			log.error("Certificate for {} has expired.", commonName);
			throw new ServiceConfigurationError(e1.getMessage(), e1);
		} catch (CertificateNotYetValidException e1) {
			log.error("Certificate for {} not yet valid.", commonName);
			throw new ServiceConfigurationError(e1.getMessage(), e1);
		}
		log.error("No certificate found for {}", commonName);
		throw new ServiceConfigurationError("No certificate found for " + commonName);
	}

	private KeyManagerFactory getKeyManagerFactory(boolean reload) {
		reload = reload || config.getClientKeyStore().isOutOfDate();
		if (keyMgrFact != null && !reload) {
			return keyMgrFact;
		}
		try {
			keyMgrFact = KeyManagerFactory.getInstance("PKIX", BCJSSE);
			keyMgrFact.init(loadKeyStore(reload), config.getClientKeyStore().getPassword().toCharArray());
			return keyMgrFact;
		} catch (GeneralSecurityException e) {
			log.error(Markers2.append(e), "Cannot create Server Key Manager: {}", e.getMessage());
			throw new ServiceConfigurationError(e.getMessage(), e);
		}
	}

	/**
	 * Adds a runnable that will be executed whenever SSL trust material has changed.
	 * @param action	The action to be performed.
	 */
	public void addSslTrustChangedListener(Runnable action) {
		trustChangedListeners.add(action);
	}
	
	public void updateTrust() {
		if (checkForUpdates()) {
			for (Runnable trustChangedListener: trustChangedListeners) {
				try {
					trustChangedListener.run();
				} catch (Exception ex) {
					log.error("Error updating trust in %s", trustChangedListener);
				}
			}
			log.info("Reloaded key trust information");
		}
	}

	boolean checkForUpdates() {
		return config.getClientKeyStore().isOutOfDate() || config.getClientTrustStore().isOutOfDate();
	}


	/**
	 * This method obtains an SNI enabled Server Socket Factory
	 * @param location	The URL to obtain a connection for
	 * @return	An HttpURLConnection that supports SNI if the URL starts with HTTPS or has no URL Scheme.
	 * @throws IOException
	 */
	public HttpURLConnection getSNIEnabledConnection(URL location) throws IOException {
		String protocol = location.getProtocol();
		if (protocol == null || !protocol.startsWith("https")) {
			// Not HTTPS
			return (HttpURLConnection) location.openConnection();
		}
		
		URLConnectionUtil util = new URLConnectionUtil(getSSLContext().getSocketFactory());
		HttpsURLConnection conx = (HttpsURLConnection) util.openConnection(location); 
		conx.setHostnameVerifier(ClientTlsSupport::verifyHostname);
		return conx;
	}
	
	/** 
	 * A method implementing the HostnameVerifier interface.
	 * TODO: This is where we can do the OCSP Checks later
	 * 
	 * @param hostName	The hostname to verify
	 * @param session	The SSLSession through which we can get more information about the host,
	 * including certificates used in the exchange.
	 * 
	 * @return	true if the Hostname is valid, false otherwise.
	 */
	private static boolean verifyHostname(String hostName, SSLSession session) {
		return true;
	}
}
