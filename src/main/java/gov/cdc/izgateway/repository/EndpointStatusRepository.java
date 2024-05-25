package gov.cdc.izgateway.repository;
import java.util.List;

import org.springframework.stereotype.Component;

import gov.cdc.izgateway.model.IDestination;
import gov.cdc.izgateway.model.IEndpointStatus;

// Technically, this is a repository, but Spring wraps a repository with a proxy that provides
// some capabilities we don't have any interest in that make it harder to debug the code.
@Component
public interface EndpointStatusRepository {
	public static final String[] INCLUDE_ALL = new String[0];
	
	List<IEndpointStatus> findAll();
	IEndpointStatus findById(String id);
	IEndpointStatus saveAndFlush(IEndpointStatus status);
	boolean removeById(String id);

	List<IEndpointStatus> find(int maxQuarterHours, String[] include);
	boolean refresh();
	IEndpointStatus newEndpointStatus();
	IEndpointStatus newEndpointStatus(IDestination dest);
}
