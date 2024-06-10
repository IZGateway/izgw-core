package gov.cdc.izgateway.service;

import gov.cdc.izgateway.model.IDestination;
import gov.cdc.izgateway.model.IEndpointStatus;

public interface IStatusCheckerService {
	void lookForReset(IDestination dest);
	void updateStatus(IEndpointStatus s, boolean wasCircuitBreakerThrown, Throwable reason);
}