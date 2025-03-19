package gov.cdc.izgateway.service;

import gov.cdc.izgateway.model.IDestination;
import gov.cdc.izgateway.model.IEndpointStatus;
import gov.cdc.izgateway.soap.fault.Fault;

/**
 * The StatusCheckerService is responsible for checking and updating the status of a destination.
 * A destination that fails status checks or has repeated failures on normal communications is marked
 * as having thrown a circuit breaker.  Destinations which have thrown a circuit breaker cannot be sent
 * messages except by those systems in the ADMIN Role. 
 * 
 * @author Audacious Inquiry
 *
 */
public interface IStatusCheckerService {
	/**
	 * Tells the service to periodically check the connectivity of the specified destination and to
	 * reset the circuit breaker when it comes back online.
	 * 
	 * @param dest	The destination whose circuit breaker was thrown.
	 */
	void lookForReset(IDestination dest);
	
	/**
	 * Update the status of the endpoint
	 * @param s	The endpoint status record.
	 * @param dest 
	 * @param reason The reason for updating status.
	 */
	void updateStatus(IEndpointStatus s, IDestination dest, Fault reason);

	/**
	 * Determine if an endpoint is exempt from status checking (some internal destinations are
	 * not connectable by design [for testing purposes], and others are always running so long 
	 * as the IZ Gateway application is running).  Other destinations may not support the 
	 * connectivity test (historically, Florida fit into that category but no longer does).
	 * 
	 * @param destId	The destination to check
	 * @return	true if the destination is one of the exempted destinations.
	 */
	boolean isExempt(String destId);

	/**
	 * Report that a circuit breaker has been reset for a given endpoint.
	 * @param status The endpoint status
	 */
	void logCircuitBreakerReset(IEndpointStatus status);

	/**
	 * Report that a circuit breaker has been thrown for a given endpoint.
	 * @param status The endpoint status
	 * @param why The reason for the status
	 */
	void logCircuitBreakerThrown(IEndpointStatus status, Throwable why);

}