package gov.cdc.izgateway.logging;

import gov.cdc.izgateway.common.HealthService;
import gov.cdc.izgateway.logging.event.EventCreator;
import gov.cdc.izgateway.logging.event.TransactionData;
import gov.cdc.izgateway.logging.info.MessageInfo;
import gov.cdc.izgateway.logging.info.SourceInfo;
import gov.cdc.izgateway.logging.markers.Markers2;
import gov.cdc.izgateway.security.service.PrincipalService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Attaches the eventId to the Mapped Diagnostic Context, enabling
 * trace between activities initiated from the same request. MDC
 * should be the highest precedence so that the eventId can be used
 * during other log sleuthing.  That makes it more important than
 * security related valves because this valve enables linkage from
 * SSL certificate checks occuring prior to initiation of the
 * HttpServletRequest object passed in to this valve.
 */
@Slf4j
@Component("valveLogging")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingValve extends LoggingValveBase implements EventCreator {
	private static final String REST_ADS = "/rest/ads";
	@SuppressWarnings("unused")
	private ScheduledFuture<?> adsMonitor =
    	Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "ADS Monitor"))
    		.scheduleAtFixedRate(this::monitorADSRequests, 0, 15, TimeUnit.SECONDS);
    private static final ConcurrentHashMap<Request, String> adsRequests = new ConcurrentHashMap<>();
    @Autowired
    public LoggingValve(PrincipalService principalService) {
        this.principalService = principalService;
    }

    @Override
    protected void handleSpecificInvoke(Request request, Response response, SourceInfo source) throws IOException, ServletException {
        boolean monitored = false;

        try {
            String who = String.format("by %s from %s", source.getCommonName(), source.getIpAddress());
            if ("POST".equals(request.getMethod()) && request.getRequestURI().startsWith(REST_ADS)) {
                log.info(Markers2.append("Source", source), "New ADS request ({}) started {}",
                		request.getCoyoteRequest().getContentLengthLong(), who);
                adsRequests.put(request, who);
                monitored = true;
            }
            
            this.getNext().invoke(request, response);
            
            TransactionData t = RequestContext.getTransactionData();
            
            if (RequestContext.getTransactionData() != null && !RequestContext.isLoggingDisabled()) {
                HealthService.incrementVolumes(t.getHasProcessError());
            }

            switch (response.getStatus()) {
            case HttpServletResponse.SC_INTERNAL_SERVER_ERROR, HttpServletResponse.SC_OK, HttpServletResponse.SC_CREATED, HttpServletResponse.SC_NO_CONTENT:
                // These are all normal responses
                break;
            case HttpServletResponse.SC_UNAUTHORIZED, HttpServletResponse.SC_SERVICE_UNAVAILABLE:
                // In these two cases, someone tried to access IZGW
                // via a URL they shouldn't have.  There was never
                // a transaction to begin with.
                RequestContext.disableTransactionDataLogging();
                break;
            default:
                if (request.getRequestURI().startsWith("/IISHubService") || request.getRequestURI().startsWith("/dev/")) {
                    // Any HTTP URI like this denotes a problem with how the request was formulated.
                    log.error("Unexpected HTTP Error {} from SOAP Request", response.getStatus());
                }
                // Do nothing.
                break;
            }
            MessageInfo messageInfo = t.getServerResponse().getWs_response_message();
            if (messageInfo != null) {
                messageInfo.setHttpHeaders(getHeaders(response));
            }

        } finally {
            if (monitored) {
                adsRequests.remove(request);
            }
        }
    }

    @Override
    protected SourceInfo setSourceInfoValues(Request req, TransactionData t) {
        SourceInfo source = super.setSourceInfoValues(req, t);

        if (req.getRequestURI().startsWith(REST_ADS)) {
            source.setType(SourceInfo.SOURCE_TYPE_ADS);
        }
        return source;
    }

    private void monitorADSRequests() {
    	for (Map.Entry<Request, String> e: adsRequests.entrySet()) {
    		reportADSProgress(e.getKey(), e.getValue());
    	}
    }

	private void reportADSProgress(Request req, String who) {
		org.apache.coyote.Request coyoteRequest = req.getCoyoteRequest();
		if (coyoteRequest == null) {
			return;
		}
		long length = coyoteRequest.getContentLengthLong();
		long bytesRead = coyoteRequest.getBytesRead();
		String percentDone = "unknown";
		if (length > 0) {
			percentDone = String.format("%0.2f%%", bytesRead * 100.0 / length);
		}
        log.info(Markers2.append("Source", RequestContext.getSourceInfo()), 
    		"ADS request by {} progress {} of {} = {}%", 
    		who,
    		convertSize(bytesRead), 
    		length < 0 ? "unknown" : convertSize(length), 
    		percentDone
        );
	}

	protected boolean isLogged(String requestURI) {
    	return requestURI.startsWith(REST_ADS) || requestURI.startsWith("/IISHubService") || requestURI.startsWith("/izgw") || requestURI.startsWith("/dev/");
	}

	@Override
	protected TransactionData createTransactionData(Request req) {
        HttpSession sess = req.getSession();

        // When IZ Gateway calls itself (e.g., for Mock access), we don't want to treat this as a new event, instead,
        // we want to retain the existing event ID to track them all together.
        Event e = getEvent(sess);
        if (e == null) {
            log.debug("{} did not get event id for : {}", req.getRequestURI(), sess.getId());
        }
        String eventId = e == null ? null : e.getId();
        req.setAttribute(EVENT_ID, eventId);
        
        // Initialize Service type.
        boolean isGateway = StringUtils.contains(req.getRequestURI(), "/IISHubService") || StringUtils.contains(req.getRequestURI(), "/rest/");

        TransactionData t = new TransactionData(eventId);
        t.setServiceType(isGateway ? "Gateway" : "Mock");
        SourceInfo source = t.getSource();
        source.setType("Unknown");
        source.setFacilityId("Unknown");

        return t;
        
	}

	@Override
	protected void clearContext() {
		// Do Nothing.
	}

}
