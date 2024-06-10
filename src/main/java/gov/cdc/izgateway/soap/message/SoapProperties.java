package gov.cdc.izgateway.soap.message;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "soap")
@Data
public class SoapProperties {
	private Map<String, Map<String, Map<String, String>>>  faults;
}
