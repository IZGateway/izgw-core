package gov.cdc.izgateway.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.cdc.izgateway.model.IDestination;
import gov.cdc.izgateway.model.IEndpointStatus;
import gov.cdc.izgateway.repository.EndpointStatusRepository;
import gov.cdc.izgateway.utils.SystemUtils;
import java.util.ArrayList;
import java.util.List;

@Service
public class EndpointStatusService {
    private final EndpointStatusRepository endpointStatusRepository;
	@Autowired
    public EndpointStatusService(EndpointStatusRepository endpointStatusRepository) {
        this.endpointStatusRepository = endpointStatusRepository;
    }
    
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
    
    public List<? extends IEndpointStatus> findAll() {
        return endpointStatusRepository.find(1, EndpointStatusRepository.INCLUDE_ALL);
    }
	public List<? extends IEndpointStatus> find(int count, String[] include) {
		return endpointStatusRepository.find(count, include);
	}

	public IEndpointStatus findById(String id) {
		return endpointStatusRepository.findById(id);
	}

	public IEndpointStatus save(IEndpointStatus status) {
		if (status == null) {
			return null;
		}
		return endpointStatusRepository.saveAndFlush(status);
	}
	
	public IEndpointStatus getEndpointStatus(IDestination dest) {
		IEndpointStatus s = this.findById(dest.getDestId());
		if (s != null) {
			return s;
		}
		return endpointStatusRepository.newEndpointStatus(dest);
	}

	public boolean refresh() {
		return endpointStatusRepository.refresh();
	}

	public boolean removeById(String id) {
		return endpointStatusRepository.removeById(id);
	}

	public void resetCircuitBreakers() {
		endpointStatusRepository.resetCircuitBreakers();
	}
}
