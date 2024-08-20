package gov.cdc.izgateway.logging;

import gov.cdc.izgateway.logging.event.EventCreator;
import gov.cdc.izgateway.logging.event.TransactionData;
import gov.cdc.izgateway.logging.info.SourceInfo;
import gov.cdc.izgateway.logging.markers.Markers2;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Globals;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.cert.X509Certificate;
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
	// Keep mappings for at most one minute.
    private static final int MAX_AGE = 60 * 1000;
    @SuppressWarnings("unused")
	private ScheduledFuture<?> adsMonitor =
    	Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "ADS Monitor"))
    		.scheduleAtFixedRate(this::monitorADSRequests, 0, 15, TimeUnit.SECONDS);
    private static final ConcurrentHashMap<Request, Integer> adsRequests = new ConcurrentHashMap<>();
    
    private Map<String, LoggingValveEvent> map = new LinkedHashMap<>();
    
    private static class LoggingValveEvent implements Event {
        private final String id;
        private Date date;
        private int refs;

        private LoggingValveEvent(String id, Date date) {
            refs = 1;
            this.id = id;
            this.date = date;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Date getDate() {
            return date;
        }
    }

    @Override
    protected void handleSpecificInvoke(Request request, Response response, SourceInfo source) throws IOException, ServletException {
        boolean monitored = false;
        if ("POST".equals(request.getMethod()) && request.getRequestURI().startsWith(REST_ADS)) {
            log.info(Markers2.append("Source", source), "New ADS request ({}) started by {} from {}",
                    request.hashCode(), source.getCommonName(), source.getIpAddress());
            adsRequests.put(request, Integer.valueOf(1));
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
    	for (Request req: adsRequests.keySet()) {
    		reportADSProgress(req);
    	}
    }

	private void reportADSProgress(Request req) {
		org.apache.coyote.Request coyoteRequest = req.getCoyoteRequest();
		if (coyoteRequest == null) {
			return;
		}
		long length = coyoteRequest.getContentLength();
		long bytesRead = coyoteRequest.getBytesRead();
		String percentDone = "unknown";
		if (length <= 0) {
			percentDone = String.format("%0.2f%%", bytesRead * 100.0 / length);
		}
        log.info(Markers2.append("Source", RequestContext.getSourceInfo()), 
    		"ADS request ({}) progress {} of {} = {}%", 
    		req.hashCode(), 
    		convertSize(bytesRead), 
    		length < 0 ? "unknown" : convertSize(length), 
    		percentDone
        );
	}

	protected boolean isLogged(String requestURI) {
    	return requestURI.startsWith(REST_ADS) || requestURI.startsWith("/IISHubService") || requestURI.startsWith("/dev/");
	}

}
