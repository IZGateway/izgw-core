package gov.cdc.izgateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration class StoreParams {
	@Value("${client.ssl.key-store}")
	private String clientKeyFile;

	@Value("${client.ssl.key-store-password:changeit}")
	private String clientKeyPassword;

	@Value("${client.ssl.key-store-provider:BCFIPS}")
	private String clientKeyStoreProvider;

	@Value("${client.ssl.key-store-type:BCFKS}")
	private String clientKeyStoreType;

	@Value("${client.ssl.trust-store}")
	private String clientTrustFile;

	@Value("${client.ssl.trust-store-password:changeit}")
	private String clientTrustPassword;

	@Value("${client.ssl.trust-store-provider:BCFIPS}")
	private String clientTrustStoreProvider;

	@Value("${client.ssl.trust-store-type:BCFKS}")
	private String clientTrustStoreType;

	public KeyStoreLoader clientKeystoreParams() {
		assert clientKeyFile != null;
		assert clientKeyPassword != null;
		assert clientKeyStoreProvider != null;
		assert clientKeyStoreType != null;
		return new KeyStoreLoader(clientKeyFile, clientKeyPassword, clientKeyStoreProvider, clientKeyStoreType);
	}

	public KeyStoreLoader clientTrustStoreParams() {
		assert clientTrustFile != null;
		assert clientTrustPassword != null;
		assert clientTrustStoreProvider != null;
		assert clientTrustStoreType != null;
		return new KeyStoreLoader(clientTrustFile, clientTrustPassword, clientTrustStoreProvider, clientTrustStoreType);
	}
}