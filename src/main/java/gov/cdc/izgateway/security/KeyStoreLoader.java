package gov.cdc.izgateway.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ServiceConfigurationError;

import com.fasterxml.jackson.annotation.JsonIgnore;

import gov.cdc.izgateway.logging.markers.Markers2;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
class KeyStoreLoader {
	@Getter
	private final String file;
	@Getter
	@JsonIgnore // should this beast ever attempt to be converted to Json, hide the password.
	private final String password;
	@Getter
	private final String provider;
	@Getter
	private final String type;

	@Getter
	private long lastUpdated = 0;
	@Getter
	private int reloadCount = 0;
	
	@Getter
	@JsonIgnore
	private KeyStore store;
	
	KeyStoreLoader(String file, String password, String provider, String type) {
		try {
			if (file == null) {
				throw new NullPointerException("file cannot be null");
			}
			if (password == null) {
				throw new NullPointerException("password cannot be null");
			}
			if (provider == null) {
				throw new NullPointerException("provider cannot be null");
			}
			if (type == null) {
				throw new NullPointerException("type cannot be null");
			}
			this.file = file;
			this.password = password;
			this.provider = provider;
			this.type = type;
			File f = new File(this.file);
			if (!f.exists()) {
				throw new IllegalArgumentException("File " + file + " does not exist");
			}
		} catch (Exception ex) {
			// Unbury the root cause in logs
			log.error("Error initializing key or trust store: {}", ex.getMessage());
			throw ex;
		}
	}
	
	long getLastModified() {
		File f = new File(file);
		if (!f.exists()) {
			return 0;
		}
		return f.lastModified();
	}
	
	void updated() {
		++reloadCount;
		this.lastUpdated = getLastModified();
	}
	
	KeyStore load() {
		return load(true);
	}
	
	KeyStore load(boolean reload) {
		reload = reload || isOutOfDate();
		if (!reload && store != null) {
			return store;
		}
		try {
			KeyStore ks = KeyStore.getInstance(getType().toUpperCase(), getProvider());
			ks.load(new FileInputStream(new File(getFile())), getPassword().toCharArray());
			updated();
			store = ks;
			return store;
		} catch (GeneralSecurityException e) {
			log.error(Markers2.append(e), "Cannot create Key Store : {}", e.getMessage());
			throw new ServiceConfigurationError(e.getMessage(), e);
		} catch (FileNotFoundException e) {
			log.error(Markers2.append(e), "Cannot open Key Store file: {}", getFile());
			throw new ServiceConfigurationError(e.getMessage(), e);
		} catch (IOException e) {
			log.error(Markers2.append(e), "Cannot read Key Store file: {}", getFile());
			throw new ServiceConfigurationError(e.getMessage(), e);
		}
	}
	
	boolean isOutOfDate() {
		return getLastModified() > lastUpdated;
	}
}