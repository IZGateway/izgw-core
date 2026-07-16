package gov.cdc.izgateway.repository;
import java.util.List;

import org.springframework.stereotype.Component;

import gov.cdc.izgateway.model.IDestination;
import gov.cdc.izgateway.model.IEndpointStatus;

// Technically, this is a repository, but Spring wraps a repository with a proxy that provides
// some capabilities we don't have any interest in that make it harder to debug the code.
@Component
public interface EndpointStatusRepository<T extends IEndpointStatus> {
	public static final String[] INCLUDE_ALL = new String[0];
	
	List<T> findAll();
	T findById(String id);
	T saveAndFlush(IEndpointStatus status);
	boolean removeById(String id);

	List<T> find(int maxQuarterHours, String[] include);
	boolean refresh();
	void resetCircuitBreakers();
	/**
	 * Reset the circuit breaker for a single endpoint by destination ID.
	 * @param id the destination ID whose circuit breaker should be reset
	 * @return the updated status, or {@code null} if no entry was found for the given ID
	 */
	T resetCircuitBreakerById(String id);
	T newEndpointStatus();
	T newEndpointStatus(IDestination dest);
}
