package gov.cdc.izgateway.soap.fault;

import gov.cdc.izgateway.common.HasDestinationUri;
import gov.cdc.izgateway.model.IDestination;
import gov.cdc.izgateway.model.RetryStrategy;
import lombok.Getter;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.tls.TlsException;
import org.bouncycastle.tls.TlsFatalAlert;
import org.bouncycastle.tls.TlsFatalAlertReceived;



/**
 * A DestinationConnectionFault records information about faults that occur when trying to connect
 * to a destination.
 * 
 * @author Audacious Inquiry
 *
 */
public class DestinationConnectionFault extends Fault implements HasDestinationUri {
    private static final long serialVersionUID = 1L;
    /** The name of this fault */
    public static final String FAULT_NAME = "DestinationConnectionFault";

    private static final String IOERR_FORMAT = "There was an IO Error %s from the destination endpoint. This may indicate a problem with the networking "
    		+ "infrastructure between IZ Gateway and the endpoint.";
    private static final MessageSupport[] MESSAGE_TEMPLATES = {
        new MessageSupport(
            FAULT_NAME,
            "10",
            "Read Timeout", null,
            "The IZ Gateway timed out waiting for the destination IIS to respond. "
                + "The IIS may be overwhelmed with requests. Retry the request again later.",
            RetryStrategy.NORMAL
        ),
        new MessageSupport(
            FAULT_NAME,
            "11",
            "Connect Timeout", null,
            "The destination service is not responding to attempts to connect to the endpoint. "
            + "Either the destination is not listening for TCP connections at this endpoint, or the connection attempt"
            + "is blocked by a Firewall or other system. "
                + "Verify connectivity (SYN, ACK, SYN-ACK) to the destination DNS address.",
            RetryStrategy.CHECK_IIS_STATUS
        ),
        new MessageSupport(
            FAULT_NAME,
            "12",
            "Connection Rejected", null,
            "The destination service is actively rejecting attempts to connect to the endpoint. "
            + "It may be blocked by a firewall, or the port may not be correctly configured in IZ Gateway. "
            + "Verify connectivity (SYN, ACK, SYN-ACK) to the destination DNS address.",
            RetryStrategy.CHECK_IIS_STATUS
        ),
        new MessageSupport(
            FAULT_NAME,
            "13",
            "DNS Resolution Error", null,
            "The TCP/IP Address of the destination IIS could not be resolved. This may be an intermittent DNS failure,"
            + "or the destination address may not be registered in DNS. "
            + "Verify that the destination address can be resolved before resending the message.",
            RetryStrategy.CHECK_IIS_STATUS
        ),
        new MessageSupport(
            FAULT_NAME,
            "14",
            "URI Syntax Error", null,
            "The url configured for this endpoint is not valid.",
            RetryStrategy.CONTACT_SUPPORT
        ),
        new MessageSupport(
            FAULT_NAME,
            "15",
            "Not HTTPS", null,
            "The url configured for this endpoint is not using https.",
            RetryStrategy.CONTACT_SUPPORT
        ),
        new MessageSupport(
            FAULT_NAME,
            "16",
            "Destination Configuration Error", null,
            "The configuration of this endpoint is not valid.",
            RetryStrategy.CONTACT_SUPPORT
        ),
        new MessageSupport(
            FAULT_NAME,
            "17",
            "Protocol Exception", null,
            "Unexpected protocol error.",
            RetryStrategy.CONTACT_SUPPORT
        ),
        new MessageSupport(
            FAULT_NAME,
            "18",
            "Circuit Breaker Thrown", "Destination Unavailable",
            "This destination has been disabled due to too many recent failures. IZ Gateway will continue to monitor"
            + " the status of this endpoint and renable it when it becomes available again.",
            RetryStrategy.CHECK_IIS_STATUS
        ),
        new MessageSupport(
            FAULT_NAME,
            "19",
            "Under Maintenance", "Destination Is Under Maintenance",
            "This destination is under maintenance",
            RetryStrategy.CHECK_IIS_STATUS
        ),
        new MessageSupport(
        	FAULT_NAME,
        	"20",
        	"Write Error", "Error writing message to the destination endpoint",
        	String.format(IOERR_FORMAT, "writing"),
        	RetryStrategy.NORMAL
        ),
        new MessageSupport(
        	FAULT_NAME,
        	"21",
        	"Read Error", "Error reading message from the destination endpoint",
        	String.format(IOERR_FORMAT, "reading"),
        	RetryStrategy.NORMAL
        ),
        new MessageSupport(
        	FAULT_NAME,
        	"22",
        	"TLS Error At IZGW", "Error establishing secure connection",
        	"There was an error establishing a trusted connection between IZ Gateway and the destination endpoint. IZ Gateway does not trust the destination endpoint.  This can result from a problem with the endpoint"
        	+ "trust parameters including supported protocol versions, encryption suites or the destination certificate.",
        	RetryStrategy.CONTACT_SUPPORT
        ),
        new MessageSupport(
        	FAULT_NAME,
        	"23",
        	"TLS Error At Destination", "Error establishing secure connection at destination",
        	"There was an error establishing a trusted connection between IZ Gateway and the destination endpoint. The destination endpoint does not trust IZ Gateway.  This can result from a problem with the destination's"
        	+ "acceptance of the IZ Gateway trust parameters including supported protocol versions, encryption suites or the destination certificate.",
        	RetryStrategy.CONTACT_SUPPORT
        ),
        new MessageSupport(
        	FAULT_NAME,
        	"24",
        	"IO Error At Destination", "Error communicating from/to destination",
        	"There was an IO Error accessing the destination endpoint. This may indicate a problem with the networking infrastructure between "
        	+ "IZ Gateway and the endpoint.",
        	RetryStrategy.NORMAL
        )
    };
    static {
    	for (MessageSupport m: MESSAGE_TEMPLATES) {
    		MessageSupport.registerMessageSupport(m);
    	}
    }
    private static final String TIMEOUT_DETAIL_FORMAT = "Timed out after %d ms";
    @Getter
	private final IDestination destination;

    protected DestinationConnectionFault() {
    	super(null);
    	destination = null;
    }
    private DestinationConnectionFault(int index, IDestination routing, String detail, Throwable rootCause) {
        super(MESSAGE_TEMPLATES[index].setDetail(computeDetail(detail, rootCause)), rootCause);
        this.destination = routing;
    }

    private static String computeDetail(String detail, Throwable rootCause) {
    	if (!StringUtils.isEmpty(detail)) {
    		return detail;
    	}
    	return rootCause == null ? null : rootCause.getMessage();
    }
    
    /**
     * Create a fault requested by an Http Header by a developer to enable 
     * developer driven fault testing of APIs.
     *  
     * @param routing	The destination for the intended fault.
     * @return	A new DestinationConnectionFault. 
     */
    public static DestinationConnectionFault devAction(IDestination routing) {
    	return timedOut(routing, new SocketTimeoutException("Timed out"), 15000);
    }

    /**
     * Create a DestinationConnectionFault from a SocketTimeout
     * 
     * @param routing	The destination
     * @param rootCause	The root cause of the fault
     * @param elapsedTimeIIS	The amount of time spent trying to connect 
     * @return	A new DestinationConnectionFault
     */
    public static DestinationConnectionFault timedOut(IDestination routing, SocketTimeoutException rootCause, long elapsedTimeIIS) {
        return new DestinationConnectionFault(
        	rootCause.getMessage().matches("^.*[Cc]onnect.*$") ? 1 : 0,
            routing, 
            String.format(TIMEOUT_DETAIL_FORMAT, elapsedTimeIIS), 
            rootCause
        );
    }

    /**
     * Create a DestinationConnectionFault from a ConnectException
     * 
     * @param routing	The destination
     * @param rootCause	The root cause of the fault
     * @param elapsedTimeIIS	The amount of time spent trying to connect 
     * @return	A new DestinationConnectionFault
     */
    public static DestinationConnectionFault connectError(IDestination routing, ConnectException rootCause, long elapsedTimeIIS) {
    	boolean timedOut = isTimeout(rootCause);
    	return new DestinationConnectionFault(timedOut ? 1 : 2, routing, timedOut ? String.format(TIMEOUT_DETAIL_FORMAT, elapsedTimeIIS) : null, rootCause);
    }
    
    /**
     * Create a DestinationConnectionFault from a SocketTimeoutException
     * 
     * @param routing	The destination
     * @param rootCause	The root cause of the fault
     * @param elapsedTimeIIS	The amount of time spent trying to connect 
     * @return	A new DestinationConnectionFault
     */
    public static DestinationConnectionFault timeoutError(IDestination routing, SocketTimeoutException rootCause, long elapsedTimeIIS) {
    	return new DestinationConnectionFault(1, routing, String.format(TIMEOUT_DETAIL_FORMAT, elapsedTimeIIS), rootCause);
    }
    
    private static boolean isTimeout(ConnectException exception) {
    	// Matches time out, timed out, Timeout, et cetera
    	String msg = exception.getMessage().toLowerCase(); 
    	return msg.contains(" time") && msg.contains("out "); 
    }

    /**
     * Create a DestinationConnectionFault from a UnknownHostException
     * 
     * @param routing	The destination
     * @param rootCause	The root cause of the fault
     * @return	A new DestinationConnectionFault
     */
    public static DestinationConnectionFault unknownHost(IDestination routing, UnknownHostException rootCause) {
    	return new DestinationConnectionFault(3, routing, null, rootCause);
    }
    
    /**
     * Create a DestinationConnectionFault from an error in the URL
     * 
     * @param routing	The destination
     * @param message The associated message
     * @param rootCause	The root cause of the fault
     * @return	A new DestinationConnectionFault
     */
    public static DestinationConnectionFault urlError(IDestination routing, String message, Throwable rootCause) {
        return new DestinationConnectionFault(4, routing, message, rootCause);
    }
    
    /**
     * Create a DestinationConnectionFault because the URL does not start with https:
     * 
     * @param routing	The destination
     * @param message The associated message
     * @param rootCause	The root cause of the fault
     * @return	A new DestinationConnectionFault
     */
    public static DestinationConnectionFault notHttps(IDestination routing, String message, Throwable rootCause) {
        return new DestinationConnectionFault(5, routing, message, rootCause);
    }
    
    /**
     * Create a DestinationConnectionFault because of a configuration error
     * 
     * @param routing	The destination
     * @param message The associated message
     * @param rootCause	The root cause of the fault
     * @return	A new DestinationConnectionFault
     */
    public static DestinationConnectionFault configurationError(IDestination routing, String message, Throwable rootCause) {
        return new DestinationConnectionFault(6, routing, message, rootCause);
    }
    
    /**
     * Create a DestinationConnectionFault because of a TUS Protocol Error error
     * 
     * @param routing	The destination
     * @param rootCause	The root cause of the fault
     * @return	A new DestinationConnectionFault
     */
    public DestinationConnectionFault tusProtocolError(IDestination routing, /*Protocol*/Exception rootCause) {
         return new DestinationConnectionFault(7, routing, "TUS Protocol Error", rootCause);
    }
    
    /**
     * Create a DestinationConnectionFault because the circuit breaker was already thrown
     * 
     * @param routing	The destination
     * @param message The associated message
     * @return	A new DestinationConnectionFault
     */
	public static DestinationConnectionFault circuitBreakerThrown(IDestination routing, String message) {
		return new DestinationConnectionFault(8, routing, message, null);
	}
	
    /**
     * Create a DestinationConnectionFault because the endpoint is under maintenance
     * 
     * @param routing	The destination
     * @return	A new DestinationConnectionFault
     */
	public static DestinationConnectionFault underMaintenance(IDestination routing) {
		return new DestinationConnectionFault(9, routing, routing.getMaintenanceDetail(), null);
	}
	
    /**
     * Create a DestinationConnectionFault because of write or TLS error while writing
     * 
     * @param routing	The destination
     * @param ex The root cause of the exception
     * @return	A new DestinationConnectionFault
     */
	public static DestinationConnectionFault writeError(IDestination routing, Throwable ex) {
		if (ex instanceof TlsException) {
			return new DestinationConnectionFault(12, routing, ex.getMessage(), ex);
		}
		return new DestinationConnectionFault(10, routing, ex.getMessage(), ex);
	}
	
	/**
     * Create a DestinationConnectionFault because of read or TLS error while reading
     * 
     * @param routing	The destination
     * @param ex The root cause of the exception
     * @return	A new DestinationConnectionFault
     */
	public static DestinationConnectionFault readError(IDestination routing, IOException ex) {
		if (ex instanceof TlsException) {
			return new DestinationConnectionFault(12, routing, ex.getMessage(), ex);
		}
		return new DestinationConnectionFault(11, routing, ex.getMessage(), ex);
	}
	
	/**
     * Create a DestinationConnectionFault because of an IO Error
     * 
     * @param routing	The destination
     * @param ex The root cause of the exception
     * @return	A new DestinationConnectionFault
     */
	public static DestinationConnectionFault ioError(IDestination routing, IOException ex) {
		return new DestinationConnectionFault(14, routing, ex.getMessage(), ex);
	}

	/**
     * Create a DestinationConnectionFault because of a TLS Error from the Destination [it's TLS implementation
     * didn't like something]
     * 
     * @param routing	The destination
     * @param tlsErr The root cause of the exception
	 * @param elapsedTimeIIS The time spend trying to connect
     * @return	A new DestinationConnectionFault
     */
	public static DestinationConnectionFault tlsErrorAtDestination(IDestination routing, TlsFatalAlertReceived tlsErr, long elapsedTimeIIS) {
		return new DestinationConnectionFault(13, routing, tlsErr.getMessage(), tlsErr);
	}
	
	/**
     * Create a DestinationConnectionFault because of a TLS Error from the IZ Gateway Server [our implementation
     * didn't like something] 
     * 
     * @param routing	The destination
     * @param tlsErr The root cause of the exception
	 * @param elapsedTimeIIS The time spend trying to connect
     * @return	A new DestinationConnectionFault
     */
	public static DestinationConnectionFault tlsErrorAtIZGW(IDestination routing, TlsFatalAlert tlsErr, long elapsedTimeIIS) {
		return new DestinationConnectionFault(12, routing, tlsErr.getMessage(), tlsErr);
	}

	@Override
	public String getDestinationId() {
		return destination == null ? null : destination.getDestId();
	}
	@Override
	public String getDestinationUri() {
		return destination == null ? null : destination.getDestUri();
	}
	/**
	 * @return true if the fault was NOT due to a thrown circuit breaker or maintenance. Used in retry logic.
	 */
	@Override
	public boolean isRetryable() {
		return super.isRetryable() && shouldBreakCircuit(); 
	}
	
	/**
	 * @return true if the fault is one which should throw the circuit breaker
	 */
	@Override
	public boolean shouldBreakCircuit() {
		return !"18".equals(getCode()) && !"19".equals(getCode());
	}
	
}
