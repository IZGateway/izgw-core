package gov.cdc.izgateway.logging;

import gov.cdc.izgateway.logging.event.EventCreator;
import gov.cdc.izgateway.logging.event.TransactionData;
import gov.cdc.izgateway.logging.info.SourceInfo;
import gov.cdc.izgateway.logging.markers.Markers2;
import gov.cdc.izgateway.security.service.PrincipalService;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
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
	public static final String EVENT_ID = "eventId";
    public static final String SESSION_ID = "sessionId";
    public static final String METHOD = "method";
    public static final String IP_ADDRESS = "ipAddress";
	public static final String REQUEST_URI = "requestUri";
	public static final String COMMON_NAME = "commonName";
	public static final List<String> MDC_EVENTS = 
		Collections.unmodifiableList(Arrays.asList(EVENT_ID, SESSION_ID, METHOD, IP_ADDRESS, REQUEST_URI, COMMON_NAME));
	
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
        String who = String.format("by %s from %s", source.getCommonName(), source.getIpAddress());
        if ("POST".equals(request.getMethod()) && request.getRequestURI().startsWith(REST_ADS)) {
        	
            log.info(Markers2.append("Source", source), "New ADS request ({}) started {}",
            		request.getCoyoteRequest().getContentLengthLong(), who);
            adsRequests.put(request, who);
            monitored = true;
        }

        try {
            this.getNext().invoke(request, response);
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
    	return requestURI.startsWith(REST_ADS) || requestURI.startsWith("/IISHubService") || requestURI.startsWith("/dev/");
	}

}
