package gov.cdc.izgateway.security;

import java.io.File;
import java.util.ServiceConfigurationError;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import gov.cdc.izgateway.logging.markers.Markers2;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Rebuilds and pushes the embedded HTTPS listener's named {@code SslBundle} when its backing
 * keystore/truststore file changes on disk, by polling file modification time rather than relying
 * on Spring Boot's own {@code reload-on-update} (inotify-based {@code WatchService}).
 * <p>
 * Inotify does not reliably detect a change written by a different client to a network
 * filesystem -- confirmed for this application's actual deployment, where the keystore lives on
 * an AWS EFS (NFSv4.1) volume (IGDD-3582). Polling file mtime is the same, already-proven-correct
 * mechanism this library already uses for the outbound client TLS path
 * ({@link ClientTlsSupport}/{@code KeyStoreLoader}).
 * <p>
 * This class only provides the check-and-reload operation; deciding how often (or whether) to run
 * it is left to the consuming application -- see {@code ServerSslBundleReloadScheduler} in
 * izgw-hub -- since polling cadence is a deployment policy decision, not something a shared
 * library should dictate to every consumer.
 */
@Component
@Slf4j
public class ServerSslBundleReloader {

	private final SslBundleRegistry sslBundleRegistry;
	private final SslBundles sslBundles;
	private final Environment environment;
	private final ResourceLoader resourceLoader;

	private volatile long lastSeenModified = 0;

	/**
	 * Create the reloader.
	 * @param sslBundleRegistry The application's SSL bundle registry, used to push a rebuilt
	 * bundle. Spring Boot registers one bean implementing both this and {@link SslBundles}.
	 * @param sslBundles The application's SSL bundles, used to look up the current bundle (to
	 * preserve its key/options/protocol across a reload).
	 * @param environment The application's environment, used to resolve {@code server.ssl.bundle}
	 * and the named bundle's {@code spring.ssl.bundle.jks.*} properties.
	 * @param resourceLoader The resource loader used to resolve keystore/truststore locations,
	 * consistent with how Spring Boot's own bundle registrar resolves them.
	 */
	public ServerSslBundleReloader(@Autowired SslBundleRegistry sslBundleRegistry, @Autowired SslBundles sslBundles,
			@Autowired Environment environment, @Autowired ResourceLoader resourceLoader) {
		this.sslBundleRegistry = sslBundleRegistry;
		this.sslBundles = sslBundles;
		this.environment = environment;
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Captures the bundle's keystore/truststore modification time at startup, so the first
	 * scheduled {@link #reloadIfChanged()} call only reacts to a change made after the bundle's
	 * own initial load -- not to the initial load itself.
	 */
	@PostConstruct
	void primeLastSeenModified() {
		lastSeenModified = currentModified();
	}

	/**
	 * Checks whether the configured bundle's keystore/truststore files have changed since the
	 * last call (or startup), and if so, rebuilds the bundle from disk and pushes it into the
	 * registry. No-ops when {@code server.ssl.bundle} is not configured -- legacy, non-bundle SSL
	 * configuration has no reload path, by design.
	 */
	public synchronized void reloadIfChanged() {
		String bundleName = bundleName();
		if (bundleName == null) {
			return;
		}
		long modified = currentModified();
		if (modified <= 0 || modified <= lastSeenModified) {
			return;
		}
		try {
			SslBundle current = sslBundles.getBundle(bundleName);
			String prefix = propertyPrefix(bundleName);
			JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails(
					environment.getProperty(prefix + ".keystore.type"),
					environment.getProperty(prefix + ".keystore.provider"),
					environment.getProperty(prefix + ".keystore.location"),
					environment.getProperty(prefix + ".keystore.password"));
			JksSslStoreDetails trustStoreDetails = new JksSslStoreDetails(
					environment.getProperty(prefix + ".truststore.type"),
					environment.getProperty(prefix + ".truststore.provider"),
					environment.getProperty(prefix + ".truststore.location"),
					environment.getProperty(prefix + ".truststore.password"));
			JksSslStoreBundle stores = new JksSslStoreBundle(keyStoreDetails, trustStoreDetails, resourceLoader);
			SslBundle updated = SslBundle.of(stores, current.getKey(), current.getOptions(), current.getProtocol());
			sslBundleRegistry.updateBundle(bundleName, updated);
			lastSeenModified = modified;
			log.info("Reloaded SSL bundle {} after keystore/truststore change", bundleName);
		} catch (Exception | ServiceConfigurationError ex) {
			log.error(Markers2.append(ex), "Failed to reload SSL bundle {}: {}", bundleName, ex.getMessage());
		}
	}

	private String bundleName() {
		String name = environment.getProperty("server.ssl.bundle");
		return StringUtils.hasText(name) ? name : null;
	}

	private String propertyPrefix(String bundleName) {
		return "spring.ssl.bundle.jks." + bundleName;
	}

	private long currentModified() {
		String bundleName = bundleName();
		if (bundleName == null) {
			return 0;
		}
		String prefix = propertyPrefix(bundleName);
		return Math.max(lastModified(environment.getProperty(prefix + ".keystore.location")),
				lastModified(environment.getProperty(prefix + ".truststore.location")));
	}

	private long lastModified(String location) {
		if (!StringUtils.hasText(location)) {
			return 0;
		}
		try {
			Resource resource = resourceLoader.getResource(location);
			File file = resource.getFile();
			return file.exists() ? file.lastModified() : 0;
		} catch (Exception ex) {
			return 0;
		}
	}
}
