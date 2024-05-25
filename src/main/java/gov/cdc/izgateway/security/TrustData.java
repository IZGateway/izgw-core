package gov.cdc.izgateway.security;

import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import gov.cdc.izgateway.common.Constants;
import gov.cdc.izgateway.logging.markers.Markers2;
import gov.cdc.izgateway.utils.X500Utils;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used to report on contents of the trust and key store to enable
 * validation of the Reload functionality.
 */
@Slf4j 
class TrustData {
	/** The name of the key or trust store file in use */
	@Getter
	private final String filename;
	/** The last update time in ms since epoch date */
	@Getter
	@JsonFormat(shape = Shape.STRING, pattern = Constants.TIMESTAMP_FORMAT)
	private final Date lastUpdated;
	/** Simplified entry contents to verify entry changes */
	@Getter
	private final Map<String, TrustData.TrustEntry> entries = new TreeMap<>();
	/** Number of times this has been reloaded */
	@Getter
	private final int reloadCount;
	/** Nested subclass for entry content */
	@Data
	private static class TrustEntry {
		/** The common name associated with the certificate subject */
		private final String commonName;
		/** The expiration date of the certificate */
		@JsonFormat(shape = Shape.STRING, pattern = Constants.TIMESTAMP_FORMAT)
		private final Date expires;
		/** The serial number of the certificate */
		private final BigInteger serialNo;
		private final String issuer;

		public TrustEntry(String commonName, Date expires, BigInteger serialNo, String issuer) {
			this.commonName = commonName;
			this.expires = expires;
			this.serialNo = serialNo;
			this.issuer = issuer;
		}

		@JsonProperty
		public String getSerialNo() {
			return serialNo.toString(16);
		}
	}

	public TrustData(KeyStoreLoader store) {
		this.filename = store.getFile();
		this.lastUpdated = new Date(store.getLastUpdated());
		this.reloadCount = store.getReloadCount();
		updateTrustMap(store.getStore(), entries);
	}

	private static void updateTrustMap(KeyStore s, Map<String, TrustData.TrustEntry> map) {
		try {
			for (String alias : Collections.list(s.aliases())) {
				X509Certificate cert = (X509Certificate) s.getCertificate(alias);
				map.put(alias, new TrustData.TrustEntry(X500Utils.getCommonName(cert), cert.getNotAfter(),
						cert.getSerialNumber(), X500Utils.getCommonName(cert.getIssuerX500Principal())));
			}
		} catch (KeyStoreException e) {
			log.error(Markers2.append(e), "Error enumerating certificates");
		}
	}
}