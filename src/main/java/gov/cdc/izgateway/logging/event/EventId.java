package gov.cdc.izgateway.logging.event;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;

import org.apache.commons.lang3.StringUtils;

import gov.cdc.izgateway.utils.SystemUtils;

@SuppressWarnings("serial")
public class EventId extends AtomicLong { // NOSONAR Singleton pattern intended
	// These values need to be initialized in this order.
    private static final String PREFIX = computeEventIdPrefix();
    private static final LongUnaryOperator NEXT_UPDATE_OP = value -> ((value < Long.MAX_VALUE) ? ++value : 1);
    /**
     * The default instance of an eventId to obtain new values from.
     */
    public static final EventId INSTANCE = new EventId();
    /**
     * The default instance of an eventId for the Status Checker main thread.
     */
    public static final String DEFAULT_TX_ID = INSTANCE.getNext();
    /**
     * The keyword used to get the eventId from the MDC
     */
    public static final String EVENTID_KEY = "eventId";
	private static boolean obfuscate = true;	// Set to false to use server ip addresses when available as first part of eventId

    private EventId() {
    }
    /**
     * Compute a prefix to prepend to event identifiers to make them unique 
     * @return	A unique prefix.
     */
    private static String computeEventIdPrefix() {
		String hostname = SystemUtils.getHostname();
		
		if (!obfuscate && hostname.contains(".")) {
			// Tie it back to host IP if we can, AWS Names are often in the form ip-10-9-97-2.ec2.internal
			String[] parts = StringUtils.substringBefore(hostname, ".").split("-");
			StringBuilder prefix = new StringBuilder();
			for (String part: parts) {
				if (StringUtils.isNumeric(part)) {
					int i = Integer.parseInt(part);
					prefix.append(String.format("%03d", i));   // Which would become 010009097002
				}
			}
			return StringUtils.stripStart(prefix.toString(), "0");
		}
		return Integer.toUnsignedString(hostname.hashCode())+"000";
	}
    
    public String getNext() {
        return PREFIX + "." + updateAndGet(NEXT_UPDATE_OP);
    }
    
    public static String getPrefix() {
    	return PREFIX;
    }
}
