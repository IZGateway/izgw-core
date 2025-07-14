package gov.cdc.izgateway;

import ch.qos.logback.classic.spi.ILoggingEvent;
import gov.cdc.izgateway.logging.MemoryAppender;
import gov.cdc.izgateway.logging.RequestContext;
import gov.cdc.izgateway.logging.event.LogEvent;
import gov.cdc.izgateway.security.AccessControlRegistry;
import gov.cdc.izgateway.security.Roles;
import gov.cdc.izgateway.soap.fault.SecurityFault;
import gov.cdc.izgateway.utils.ListConverter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collections;
import java.util.List;

/**
 * The LogController provides access to in memory logging data on a server.
 * This is used for integration testing to verify log content is as expected
 * when sending messages.
 */

// TODO: Presently, blacklisted users are allowed to access the logs request, b/c blacklisting only
// applies to the SOAP Stack.  Once we apply it to the full HTTP stack, we will have to provide
// SECURE mechanism for clearing the blacklisted state of the testing user.  It cannot be said
// to have been applied to the full stack until this loophole is resolved.
public class LogControllerBase implements InitializingBean {

	private MemoryAppender logData = null;

	protected LogControllerBase() {
	}

	@Override
	public void afterPropertiesSet() {
		logData = MemoryAppender.getInstance("memory");
	}

	// TODO: Presently, blacklisted users are allowed to access the logs request, b/c blacklisting only
	// applies to the SOAP Stack.  Once we apply it to the full HTTP stack, we will have to provide
	// SECURE mechanism to clearing the state.
	protected List<LogEvent> getLogs(String search) {

		List<ILoggingEvent> events;
		if (logData == null) {
			events = Collections.emptyList();
		} else if (StringUtils.isBlank(search)) {
			events = logData.getLoggedEvents();
		} else {
			events = logData.search(search);
		}
		
		return new ListConverter<>(events, LogEvent::new);
	}

	public void deleteLogs(HttpServletRequest servletReq, String clear) throws SecurityFault {
        if (!RequestContext.getRoles().contains(Roles.ADMIN) && !RequestContext.getRoles().contains(Roles.OPERATIONS)) {
            throw SecurityFault
                    .generalSecurity("Delete Log Attempt By Role",
                            RequestContext.getRoles().toString(), null);
        }

		if (logData != null) {
			logData.reset();
		}
	}
	
}
