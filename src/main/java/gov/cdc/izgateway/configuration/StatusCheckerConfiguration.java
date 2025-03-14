package gov.cdc.izgateway.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import gov.cdc.izgateway.service.IStatusCheckerService;
import lombok.Data;

/**
 * This class contains information affecting the configuration of the StatusChecker component.  
 * @see IStatusCheckerService
 * @author Audacious Inquiry
 */
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
    @Value("${hub.status-check.exemptions:}")
    private List<String> exempt;
    /** List of endpoints EXPECTED to fail */
    @Value("${hub.status-check.failing-endpoints: 404,down,invalid,reject}")
    private List<String> testingEndpoints;
}