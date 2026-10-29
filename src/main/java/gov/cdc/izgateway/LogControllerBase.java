package gov.cdc.izgateway;

import ch.qos.logback.classic.spi.ILoggingEvent;
import gov.cdc.izgateway.logging.MemoryAppender;
import gov.cdc.izgateway.logging.RequestContext;
import gov.cdc.izgateway.logging.event.LogEvent;
import gov.cdc.izgateway.security.Roles;
import gov.cdc.izgateway.soap.fault.SecurityFault;
import gov.cdc.izgateway.utils.ListConverter;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

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
public class LogControllerBase {

	protected LogControllerBase() {
	}

	// TODO: Presently, blacklisted users are allowed to access the logs request, b/c blacklisting only
	// applies to the SOAP Stack.  Once we apply it to the full HTTP stack, we will have to provide
	// SECURE mechanism to clearing the state.
	protected List<LogEvent> getLogs(String search) {
		MemoryAppender logData = MemoryAppender.getInstance("memory");

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

	/**
	 * Delete the logs
	 * @param servletReq	The servlet request
	 * @param clear		The clear parameter
	 * @throws SecurityFault	If the user is not authorized
	 */
	public void deleteLogs(HttpServletRequest servletReq, String clear) throws SecurityFault {
        if (!RequestContext.getRoles().contains(Roles.ADMIN) && !RequestContext.getRoles().contains(Roles.OPERATIONS)) {
            throw SecurityFault
                    .generalSecurity("Delete Log Attempt By Role",
                            RequestContext.getRoles().toString(), null);
        }

		MemoryAppender logData = MemoryAppender.getInstance("memory");
		if (logData != null) {
			logData.reset();
		}
	}

}
