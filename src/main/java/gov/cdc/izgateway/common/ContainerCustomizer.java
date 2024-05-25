package gov.cdc.izgateway.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

import gov.cdc.izgateway.logging.LoggingValve;
import gov.cdc.izgateway.security.AccessControlValve;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ContainerCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

	private LoggingValve loggingValve;
	private AccessControlValve accessControlValve;
	
	 @Autowired
	 public ContainerCustomizer(LoggingValve loggingValve, AccessControlValve accessControlValve) {
		this.loggingValve = loggingValve;
		this.accessControlValve = accessControlValve;
	}
	
    @Override
    public void customize(TomcatServletWebServerFactory factory) {

        log.info("Configuring embedded Tomcat");
        factory.addContextValves(loggingValve, accessControlValve);
    }
}