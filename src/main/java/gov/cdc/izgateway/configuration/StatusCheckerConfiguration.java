package gov.cdc.izgateway.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@Data
public class StatusCheckerConfiguration {
    /** Max number of retries to attempt before considering the request failed */
    @Value("${hub.status-check.maxfailures:3}")
	private int maxFailuresBeforeCircuitBreaker;
    /** Period between status checks */
    @Value("${hub.status-check.period:5}")
    private int statusCheckPeriodInMinutes;
    /** List of endpoints exempt from status checks */
    @Value("${hub.status-check.exemptions:fl}")
    private List<String> exempt;
    /** List of endpoints EXPECTED to fail */
    @Value("${hub.status-check.failing-endpoints: 404,down,invalid,reject}")
    private List<String> testingEndpoints;
}