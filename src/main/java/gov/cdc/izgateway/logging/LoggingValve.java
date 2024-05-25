package gov.cdc.izgateway.logging;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.apache.catalina.Globals;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import gov.cdc.izgateway.common.HealthService;
import gov.cdc.izgateway.logging.event.EventCreator;
import gov.cdc.izgateway.logging.event.EventId;
import gov.cdc.izgateway.logging.event.TransactionData;
import gov.cdc.izgateway.logging.info.MessageInfo;
import gov.cdc.izgateway.logging.info.SourceInfo;
import gov.cdc.izgateway.logging.markers.Markers2;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLSession;

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
public class LoggingValve extends ValveBase implements EventCreator {
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
    
    private static class LoggingValveEvent implements EventCreator.Event {
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
    public void invoke(Request req, Response resp) throws IOException, ServletException {
        HttpSession sess = req.getSession();
        // When IZ Gateway calls itself (e.g., for Mock access), we don't want to treat this as a new event, instead,
        // we want to retain the existing event ID to track them all together.
        Event e = getEvent(sess);
        if (e == null) {
            log.debug("{} did not get event id for : {}", req.getRequestURI(), sess.getId());
        }
        
        fixHeaders(req);

        // If no event was created for this session, go ahead and create a new one.
        TransactionData t = new TransactionData(e == null ? null : e.getId());
        // Initialize Service type.
        String requestURI = req.getRequestURI();
        boolean isGateway = StringUtils.contains(requestURI, "/IISHubService") || StringUtils.contains(requestURI, "/rest/");
    	t.setServiceType(isGateway ? "Gateway" : "Mock");
    	
        SourceInfo source = setSourceInfoValues(req, t);
        setMdcValues(req, sess, t, requestURI, source);
        // Also put it into the request so that other threads handling work on behalf
        // of it can get to it.
        req.setAttribute(EventId.EVENTID_KEY, t.getEventId());
        // Put the event id in the response header for traceability.
    	RequestContext.setTransactionData(t);
    	RequestContext.setHttpHeaders(getHeaders(req));
    	RequestContext.setResponse(resp);
    	if (!isLogged(requestURI)) {
        	RequestContext.disableTransactionDataLogging();
    	}

    	boolean monitored = false;
        if ("POST".equals(req.getMethod()) && requestURI.startsWith(REST_ADS)) {
            log.info(Markers2.append("Source", source), "New ADS request ({}) started by {} from {}", 
            		req.hashCode(), source.getCommonName(), source.getIpAddress());
        	adsRequests.put(req, Integer.valueOf(1));
        	monitored = true;
        }

        try {
        	Response r = resp;
        	try {
        		this.getNext().invoke(req, r);
        	} finally {
        		if (monitored) {
        			adsRequests.remove(req);
        		}
        	}
            
            switch (resp.getStatus()) {
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
            	if (requestURI.startsWith("/IISHubService") || requestURI.startsWith("/dev/")) {
            		// Any HTTP URI like this denotes a problem with how the request was formulated.
            		log.error("Unexpected HTTP Error {} from SOAP Request", resp.getStatus());
            	}
                // Do nothing.
                break;
            }
        } catch (Exception ex) {
            log.error(Markers2.append(ex), "Uncaught Exception during invocation");
        } catch (Error err) {  // NOSONAR OK to Catch Error here
            log.error(Markers2.append(err), "Error during invocation");
        } finally {
            // Log first, then clean up MDC!
            if (RequestContext.getTransactionData() != null && !RequestContext.isLoggingDisabled()) {
            	MessageInfo messageInfo = t.getServerResponse().getWs_response_message();
            	if (messageInfo != null) {
            		messageInfo.setHttpHeaders(getHeaders(resp));
            	}
                t.logIt();
                HealthService.incrementVolumes(t.getHasProcessError());
            } 
            RequestContext.clear();
            clearMdcValues();
        }
    }
    
    private void monitorADSRequests() {
    	for (Request req: adsRequests.keySet()) {
    		reportADSProgress(req);
    	}
    }
    
    private String convertSize(long sizeBytes) {
    	if (sizeBytes <= 0) {
    		return "0b";
    	}
    	String[] sizeName = {"b", "Kb", "Mb", "Gb"};
    	int i = (int)Math.floor(Math.log(sizeBytes)/Math.log(1024));
		double p = Math.pow(1024, i);
		return String.format("%0.2f %s", sizeBytes / p, sizeName[i]);
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

	private boolean isLogged(String requestURI) {
    	return requestURI.startsWith(REST_ADS) || requestURI.startsWith("/IISHubService") || requestURI.startsWith("/dev/");
	}

	private Map<String, List<String>> getHeaders(Response resp) {
    	Map<String, List<String>> headers = new TreeMap<>();
    	for (String name: resp.getHeaderNames()) {
			List<String> l = new ArrayList<>();
			for (String v: resp.getHeaders(name)) {
    			l.add(v);
    		}
    		headers.put(name, l);
    	}
    	return headers;
	}

	private Map<String, List<String>> getHeaders(Request req) {
    	Map<String, List<String>> headers = new TreeMap<>();
    	for (Enumeration<String> h = req.getHeaderNames(); h.hasMoreElements(); ) {
    		String name = h.nextElement();
			List<String> l = new ArrayList<>();
    		for (Enumeration<String> v = req.getHeaders(name); v.hasMoreElements(); ) {
    			l.add(v.nextElement());
    		}
    		headers.put(name, l);
    	}
    	return headers;
	}

	/**
     * This method is to deal with invalid headers, which IZ Gateway 1.X versions simply ignored.
     * @param req The request to check and adjust if needed.
     */
    
	private void fixHeaders(Request req) {
		fixHeader(req, HttpHeaders.ACCEPT, t -> StringUtils.contains(t, "/"), "*/*"); 
		fixHeader(req, HttpHeaders.ACCEPT_CHARSET, t-> "UTF-8".equalsIgnoreCase(t) || checkCharset(t), "utf-8"); 
	}
	
	/**
	 * Verify that a header on the request is valid, and replace it with a default value if not.
	 * @param req	The request to check
	 * @param header	The name of the header to verify
	 * @param test	The test to perform for validation
	 * @param replacement	The replacement text to substitute or null to simply remove the header.
	 */
	private void fixHeader(Request req, String header, Predicate<String> test, String replacement) {
		Enumeration<String> headers = req.getHeaders(header);
		while (headers.hasMoreElements()) {
			String value = headers.nextElement();
			if (!test.test(value)) {
				// A bogus header value was found.
				req.getCoyoteRequest().getMimeHeaders().removeHeader(header);
				if (replacement != null && replacement.length() != 0) {
					req.getCoyoteRequest().getMimeHeaders().addValue(header).setString(replacement);
				}
				return;
			}
		}
	}
	
	/**
	 * Charset name tester
	 * @param charsetName	The charset to check.
	 * @return True if it's a charset known to the JVM.
	 */
	private boolean checkCharset(String charsetName) {
		try {
			Charset.forName(charsetName);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void setMdcValues(Request req, HttpSession sess, TransactionData t, String requestURI, SourceInfo source) {
		// Put the id into thread local storage so that threaded events can get to it
        MDC.put(EventId.EVENTID_KEY, t.getEventId());
        MDC.put(SESSION_ID, sess.getId());
        MDC.put(REQUEST_URI, requestURI);
        MDC.put(METHOD, req.getMethod());
        MDC.put(IP_ADDRESS, req.getRemoteAddr());
        MDC.put(COMMON_NAME, source.getCommonName());
	}
	
	private void clearMdcValues() {
        // Remove Added MDC Keys
        MDC.remove(EventId.EVENTID_KEY);
        MDC.remove(SESSION_ID);
        MDC.remove(REQUEST_URI);
        MDC.remove(METHOD);
        MDC.remove(IP_ADDRESS);
        MDC.remove(COMMON_NAME);
    }

	private SourceInfo setSourceInfoValues(Request req, TransactionData t) {
		SourceInfo source = t.getSource();
		source.setCipherSuite((String) req.getAttribute(Globals.CIPHER_SUITE_ATTR));
		source.setHost(req.getRemoteHost());
		source.setIpAddress(req.getRemoteAddr());
		source.setType("Unknown");
		source.setFacilityId("Unknown");
		
        X509Certificate[] certs = (X509Certificate[])req.getAttribute(Globals.CERTIFICATES_ATTR);
        if (certs != null) {
            source.setCertificate(certs[0]);
        }
        if (req.getRequestURI().startsWith(REST_ADS)) {
            source.setType(SourceInfo.SOURCE_TYPE_ADS);
        }
		return source;
	}

	public Event getEvent(HttpSession sess) {
        String sessionId = sess.getId();
        LoggingValveEvent event = map.get(sessionId);

        if (event != null && --event.refs == 0) {
            map.remove(sessionId);
        }

        // Eventually, events will age out
        if (map.size() > 20) {
            // Purge the cache of values that are older than MAX_AGE
            long time = System.currentTimeMillis() - MAX_AGE;
            Iterator<Entry<String, LoggingValveEvent>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, LoggingValveEvent> e = it.next();
                if (e.getValue().getDate().getTime() > time) {
                    break;
                }
                it.remove();
            }
        }

        return event;
    }

    @Override
    public String createEvent(SSLSession sess) {
        LoggingValveEvent event = map.get(EventCreator.toHex(sess.getId()));

        if (event != null) {
            event.date = new Date(); // reset the date
            event.refs++;   // Bump the ref count
        } else {
            event = new LoggingValveEvent(TransactionData.getNextEventId(), new Date());
            map.put(EventCreator.toHex(sess.getId()), event);
        }

        return event.getId();
    }
}
