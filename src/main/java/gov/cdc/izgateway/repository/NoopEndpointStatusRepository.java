package gov.cdc.izgateway.repository;

import gov.cdc.izgateway.model.AbstractEndpointStatus;
import gov.cdc.izgateway.model.IDestination;
import gov.cdc.izgateway.model.IEndpointStatus;
import gov.cdc.izgateway.service.IJurisdictionService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * A no-operation implementation of the EndpointStatusRepository interface.
 */
@Repository
@ConditionalOnMissingBean(EndpointStatusRepository.class)
public class NoopEndpointStatusRepository implements EndpointStatusRepository<IEndpointStatus> {
	
	/**
	 * A no-operation implementation of the IEndpointStatus interface.
	 */
	public static class EndpointStatus extends AbstractEndpointStatus {

		@Override
		public IEndpointStatus copy() {
			// This is a no-op
			return null;
		}

		@Override
		public IJurisdictionService getJurisdictionService() {
			// This is a no-op
			return null;
		}
	}
    private IEndpointStatus status = new EndpointStatus();

    @Override
    public List<IEndpointStatus> findAll() {
        return List.of();
    }

    @Override
    public IEndpointStatus findById(String id) {
        return status;
    }

    @Override
    public IEndpointStatus saveAndFlush(IEndpointStatus status) {
        return status;
    }

    @Override
    public boolean removeById(String id) {
        return false;
    }

    @Override
    public List<IEndpointStatus> find(int maxQuarterHours, String[] include) {
        return List.of();
    }

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public void resetCircuitBreakers() {
    	// Do nothing
    }

    @Override
    public IEndpointStatus newEndpointStatus() {
        return status;
    }

    @Override
    public IEndpointStatus newEndpointStatus(IDestination dest) {
        return status;
    }
}
