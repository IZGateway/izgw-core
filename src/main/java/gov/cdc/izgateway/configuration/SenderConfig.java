package gov.cdc.izgateway.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "hub")
@Data
public class SenderConfig {
	int maxMessageSize;
	int maxRetries;
}
