package gov.cdc.izgateway.logging;

import gov.cdc.izgateway.common.HealthService;
import gov.cdc.izgateway.logging.event.EventCreator;
import gov.cdc.izgateway.logging.event.EventId;
import gov.cdc.izgateway.logging.event.TransactionData;
import gov.cdc.izgateway.logging.info.MessageInfo;
import gov.cdc.izgateway.logging.info.SourceInfo;
import gov.cdc.izgateway.logging.markers.Markers2;
import gov.cdc.izgateway.security.IzgPrincipal;
import gov.cdc.izgateway.security.service.PrincipalService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Globals;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;

/**
 * The base valve for configuring logging.
 * 
 * @author Audacious Inquiry
 */
@Slf4j
public abstract class LoggingValveBase extends ValveBase implements EventCreator {
	/** The MDC key for the event identifier */
	public static final String EVENT_ID = "eventId";
	/** The MDC key for the session identifier */
    public static final String SESSION_ID = "sessionId";
	/** The MDC key for the request method identifier */
    public static final String METHOD = "method";
	/** The MDC key for the IP address of the sender */
    public static final String IP_ADDRESS = "ipAddress";
	/** The MDC key for the request URI */
	public static final String REQUEST_URI = "requestUri";
	/** The MDC key for the common name of the associated certificate */
	public static final String COMMON_NAME = "commonName";
	/** The MDC Events to save and restore when overriding them */
	public static final List<String> MDC_EVENTS = 
		Collections.unmodifiableList(Arrays.asList(EVENT_ID, SESSION_ID, METHOD, IP_ADDRESS, REQUEST_URI, COMMON_NAME));

    protected PrincipalService principalService;

    // Keep mappings for at most one minute.
    private static final int MAX_AGE = 60 * 1000;
    // HTTP Headers for IP Address, Host and Protocol values
	private static final String X_FORWARDED_FOR = "x-forwarded-for";

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

    protected static String convertSize(long sizeBytes) {
    	if (sizeBytes <= 0) {
    		return "0b";
    	}
    	String[] sizeName = {"b", "Kb", "Mb", "Gb"};
    	int i = (int)Math.floor(Math.log(sizeBytes)/Math.log(1024));
		double p = Math.pow(1024, i);
		return String.format("%0.2f %s", sizeBytes / p, sizeName[i]);
    }

    @Override
    public final void invoke(Request req, Response resp) throws IOException, ServletException {
        fixHeaders(req);

        // If no event was created for this session, go ahead and create a new one.
        TransactionData t = createTransactionData(req);

        SourceInfo source = setSourceInfoValues(req, t);
        setMdcValues(req, t, req.getRequestURI(), source);

        // Set RequestContext values.
        IzgPrincipal p = principalService.getPrincipal(req);
        RequestContext.setPrincipal(p);
        RequestContext.setTransactionData(t);
        RequestContext.setHttpHeaders(getHeaders(req));
        RequestContext.setResponse(resp);
        
        // Update the principal in source
        t.getSource().setPrincipal(p);
        
        if (!isLogged(req.getRequestURI())) {
            RequestContext.disableTransactionDataLogging();
        }

        try {
            handleSpecificInvoke(req, resp, source);
        } catch (Exception ex) {
            log.error(Markers2.append(ex), "Uncaught Exception during invocation", ex);
        } catch (Error err) {  // NOSONAR OK to Catch Error here
            log.error(Markers2.append(err), "Error during invocation", err);
        } finally {
            // Log first, then clean up MDC!
            if (RequestContext.getTransactionData() != null && !RequestContext.isLoggingDisabled()) {
                t.logIt();
            }
            RequestContext.clear();
            clearMdcValues();
            clearContext();
        }
    }

    protected abstract void clearContext();

	protected abstract TransactionData createTransactionData(Request req);

	protected abstract void handleSpecificInvoke(Request request, Response response, SourceInfo source) throws IOException, ServletException ;

	protected abstract boolean isLogged(String requestURI);

    protected static Map<String, List<String>> getHeaders(Response resp) {
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

	protected static Map<String, List<String>> getHeaders(Request req) {
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
    
	protected void fixHeaders(Request req) {
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
	protected void fixHeader(Request req, String header, Predicate<String> test, String replacement) {
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
	protected boolean checkCharset(String charsetName) {
		try {
			Charset.forName(charsetName);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	protected void setMdcValues(Request req, TransactionData t, String requestURI, SourceInfo source) {
		// Put the id into thread local storage so that threaded events can get to it
        MDC.put(EventId.EVENTID_KEY, t.getEventId());
        MDC.put(SESSION_ID, req.getSession().getId());
        MDC.put(REQUEST_URI, requestURI);
        MDC.put(METHOD, req.getMethod());
        MDC.put(IP_ADDRESS, req.getRemoteAddr());
        MDC.put(COMMON_NAME, source.getCommonName());
	}
	
	protected void clearMdcValues() {
        // Remove Added MDC Keys
        MDC.remove(EventId.EVENTID_KEY);
        MDC.remove(SESSION_ID);
        MDC.remove(REQUEST_URI);
        MDC.remove(METHOD);
        MDC.remove(IP_ADDRESS);
        MDC.remove(COMMON_NAME);
    }

    protected SourceInfo setSourceInfoValues(Request req, TransactionData t) {

        SourceInfo source = t.getSource();
        source.setCipherSuite((String) req.getAttribute(Globals.CIPHER_SUITE_ATTR));
        source.setHost(req.getRemoteHost());
        source.setIpAddress(
        	StringUtils.substringBefore(
    			StringUtils.defaultIfEmpty(
    				req.getHeader(X_FORWARDED_FOR), 
    				req.getRemoteAddr()
    			),
    			","
        	).trim()
        );
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
