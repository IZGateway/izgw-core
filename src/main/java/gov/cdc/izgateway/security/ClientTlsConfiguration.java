package gov.cdc.izgateway.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@Data 
public class ClientTlsConfiguration {

	private static final String DEFAULT_CIPHERS = "TLS_AES_256_GCM_SHA384,"
			+ "TLS_AES_128_GCM_SHA256,"
			+ "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,"
			+ "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,"
			+ "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,"
			+ "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,"
			+ "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,"
			+ "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384,"
			+ "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,"
			+ "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256";
	private static final String DEFAULT_PROTOCOLS = "TLSv1.2,TLSv1.3";

	private final KeyStoreLoader clientKeyStore;
	private final KeyStoreLoader clientTrustStore;

	@Value("${client.hostname:}")
	private String clientHostname;

	@Value("${client.ssl.monitoringPeriod:10}")
	private int monitoringPeriod;

	@Value("${client.ssl.ciphers:" + DEFAULT_CIPHERS + "}")
	private String cipherSuites;

	@Value("${client.ssl.enabled-protocols:" + DEFAULT_PROTOCOLS + "}")
	private String protocols;

	@Value("${security.enable-ocsp:false}")
	private boolean ocspEnabled;

	@Value("${server.hostname}")
	private String serverName;
	
	@Value("${client.ssl.debug:false}")
	private boolean sslDebug;
	
	@Autowired
	public ClientTlsConfiguration(StoreParams params) {
		clientKeyStore = params.clientKeystoreParams();
		clientTrustStore = params.clientTrustStoreParams();
	}

}