package gov.cdc.izgateway.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.cdc.izgateway.model.IDestination;
import gov.cdc.izgateway.model.IEndpointStatus;
import gov.cdc.izgateway.repository.EndpointStatusRepository;
import gov.cdc.izgateway.utils.SystemUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * The EndpointStatusService enables management to a stored cache of endpoint statuses for each destination
 * accessible by the given host.
 * 
 * @author Audacious Inquiry
 *
 */
@Service
public class EndpointStatusService {
    private final EndpointStatusRepository<? extends IEndpointStatus> endpointStatusRepository;
	/**
	 * Create a new service instance
	 * @param endpointStatusRepository	The repository used to store the status values.
	 */
	@Autowired
    public EndpointStatusService(EndpointStatusRepository<? extends IEndpointStatus> endpointStatusRepository) {
        this.endpointStatusRepository = endpointStatusRepository;
    }
    
    /**
     * @return The list of hosts storing endpoint status in this repository
     */
    public List<String> getHosts() {
    	List<String> l = new ArrayList<>();
		l.add(SystemUtils.getHostname());
    	for (IEndpointStatus s: findAll()) {
    		if (!l.contains(s.getStatusBy())) {
    			l.add(s.getStatusBy());
    		}
    	}
    	return l;
    }
    
    /**
     * @return All endpoint status records known to the system
     */
    public List<? extends IEndpointStatus> findAll() {
        return endpointStatusRepository.find(1, EndpointStatusRepository.INCLUDE_ALL);
    }
    
	/**
	 * Get the endpoint status for the specified endpoints and the number of recent entries.
	 * @param count	The number of recent entries to include
	 * @param include	The endpoints to include.
	 * @return The list of status entries found.
	 */
	public List<? extends IEndpointStatus> find(int count, String[] include) {
		return endpointStatusRepository.find(count, include);
	}

	/**
	 * Get a specific status entry for a destination.
	 * @param destId the destination identifier
	 * @return	The status entry
	 */
	public IEndpointStatus findById(String destId) {
		return endpointStatusRepository.findById(destId);
	}

	/**
	 * Save the current endpoint status
	 * @param status	The status to save
	 * @return The saved status
	 */
	public IEndpointStatus save(IEndpointStatus status) {
		if (status == null) {
			return null;
		}
		return endpointStatusRepository.saveAndFlush(status);
	}
	
	/**
	 * Get the endpoint status for a specific destination.
	 * @param dest The destination to get the status of
	 * @return The status of that destination
	 */
	public IEndpointStatus getEndpointStatus(IDestination dest) {
		IEndpointStatus s = this.findById(dest.getDestId());
		if (s != null) {
			return s;
		}
		return endpointStatusRepository.newEndpointStatus(dest);
	}

	/**
	 * Refresh the local cache from the endpointStatusRepository 
	 * @return True if the refresh was successful, false otherwise.
	 */
	public boolean refresh() {
		return endpointStatusRepository.refresh();
	}

	/**
	 * @param id Remove an entry
	 * @return	true if the entry was removed
	 */
	public boolean removeById(String id) {
		return endpointStatusRepository.removeById(id);
	}

	/**
	 * Reset the circuit breakers for any entry that has them thrown.
	 */
	public void resetCircuitBreakers() {
		endpointStatusRepository.resetCircuitBreakers();
	}
}
