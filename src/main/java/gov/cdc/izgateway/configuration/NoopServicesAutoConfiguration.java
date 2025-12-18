package gov.cdc.izgateway.configuration;

import gov.cdc.izgateway.service.IMessageHeaderService;
import gov.cdc.izgateway.service.NoopMessageHeaderService;
import gov.cdc.izgateway.service.IAccessControlService;
import gov.cdc.izgateway.security.NoopAccessControlService;
import gov.cdc.izgateway.repository.EndpointStatusRepository;
import gov.cdc.izgateway.repository.NoopEndpointStatusRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 
 * @author Audacious Inquiry
 *
 */
@Configuration
public class NoopServicesAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(IMessageHeaderService.class)
    public IMessageHeaderService noopMessageHeaderService() {
        return new NoopMessageHeaderService();
    }

    @Bean
    @ConditionalOnMissingBean(IAccessControlService.class)
    public IAccessControlService noopAccessControlService() {
        return new NoopAccessControlService();
    }

    @Bean
    @ConditionalOnMissingBean(EndpointStatusRepository.class)
    public EndpointStatusRepository<?> noopEndpointStatusRepository() {
        return new NoopEndpointStatusRepository();
    }
}
