package gov.cdc.izgateway.configuration;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "client")
@Data
public class ClientConfiguration implements InitializingBean {
	private int readTimeout = 60000;
	private int connectTimeout = 15000;
	private int maxBufferSize = 65536;
	private int maxRetries = 3;
	private String contentType = MediaType.TEXT_XML_VALUE;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		// TODO Read trust and key store
	}
}
