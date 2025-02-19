package gov.cdc.izgateway.logging.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

import gov.cdc.izgateway.common.Constants;
import gov.cdc.izgateway.configuration.AppProperties;
import gov.cdc.izgateway.logging.info.DestinationInfo;
import gov.cdc.izgateway.logging.info.MessageInfo.RequestInfo;
import gov.cdc.izgateway.logging.info.MessageInfo.ResponseInfo;
import gov.cdc.izgateway.logging.markers.MarkerObjectFieldName;
import gov.cdc.izgateway.logging.markers.Markers2;
import gov.cdc.izgateway.soap.fault.Fault;
import gov.cdc.izgateway.soap.fault.FaultSupport;
import gov.cdc.izgateway.soap.fault.HubClientFault;
import gov.cdc.izgateway.soap.fault.UnexpectedExceptionFault;
import gov.cdc.izgateway.soap.fault.UnsupportedOperationFault;
import gov.cdc.izgateway.logging.info.SourceInfo;
import gov.cdc.izgateway.utils.HL7Utils;
import gov.cdc.izgateway.utils.HL7Utils.HL7Message;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@JsonPropertyOrder(value = {
		"transactionId", "eventId", "messageId", "dateTime", "source", "destination",
		// This ordering simplifies retention of log ordering in IZGW 1.X of web service messages
		"serverRequest", "clientRequest", "clientResponse", "serverResponse"
	}, alphabetic=true)
@Schema(
description = "Records information about API requests. There should be a transactionData entry in logs for each request routed by IZ Gateway.\n "
 			+ "This class holds information gathered when processing a soap message in IisHubServiceImpl.java. "
 			+ "Rather than output log messages as we go, we collect that info in an instance of this class, and "
 			+ "then write this data out as a json object at the end of the process. "
 			+ "(We do this since the SOAP processing is multi-threaded (multiple messages being processed at once), "
 			+ "so the log file would have logging messages from different processes all mixed together. Really tough to "
 			+ "troubleshoot.  Using this class, the info for one soap message is logged in a single log message.)")
@Data
@MarkerObjectFieldName("transactionData")
@JsonInclude(Include.ALWAYS)
public class TransactionData {
    // Types of SOAP messages we encounter
    public enum MessageType {
        SUBMIT_FILE("ads"), SUBMIT_SINGLE_MESSAGE("submitSingleMessage"), CONNECTIVITY_TEST("connectivityTest"), OTHER("Other"), INVALID_REQUEST("invalidRequest");
        private final String name;
        private MessageType(String name) {
        	this.name = name;
        }
        @JsonValue
        @Override
        public String toString() {
        	return name;
        }
        
        public MessageType fromString(String value) {
        	for (MessageType v: MessageType.values()) {
        		if (v.toString().equalsIgnoreCase(value)) {
        			return v;
        		}
        	}
        	return OTHER;
        }
    }
    
    // Types of HL7 messages we encounter
    public enum RequestPayloadType {
    	  QBP("QBP"), VXU("VXU"), OTHER("Other"), COVIDALL("covidallMonthlyVaccination"),
        FLU("influenzaVaccination"), RI("routineImmunization"), RSV("rsvPrevention"), UNKNOWN("Unknown");
        private final String name;
        private RequestPayloadType(String name) {
        	this.name = name;
        }
        @JsonValue
        @Override
        public String toString() {
        	return name;
        }
        
        public static RequestPayloadType fromString(String value) {
        	for (RequestPayloadType v: RequestPayloadType.values()) {
        		if (v.toString().equalsIgnoreCase(value)) {
        			return v;
        		}
        	}
        	return OTHER;
        }
    }

    // For truncating HL7 messages when in prod mode...
    public static final int HL7_TRIM_SIZE = 52;
    public static final int HL7_ERR_TRIM_SIZE = 255;

    // HL7 error patterns
    // It's an error if we get the response contains a rejection
    public static final String HL7_REJECT_ERROR = "(MSA\\|(CR|AR))";
    // Matches an HL7 Field in a message
    public static final String HL7_ANY_FIELD = "\\|[^\\|]*";
    // Matches an ERR Segment with ERR-4-1 == "E"
    public static final String HL7_ERR_ERROR = "(ERR" + HL7_ANY_FIELD + HL7_ANY_FIELD + HL7_ANY_FIELD + "\\|E)";
    // Matches any message that matches either the reject or the ERR error patterns.
    public static final Pattern HL7_ERROR_PATTERN = Pattern.compile("(" + HL7_REJECT_ERROR + "|" + HL7_ERR_ERROR + ")");

    // Date and time formatters

    // Data - in no logical order
    @Schema(description="A unique id associated with this transaction", format="UUID")
    @JsonProperty
    private final String transactionId;                  // Internally generated unique id - a uuid
    @Schema(description="The unique event id associated with this transaction. This identifier will be the same for all log events associated with a single transaction. See also LogEvent.eventId", format="\\d+\\.\\d+")
    @JsonProperty
    private final String eventId;
    @Schema(description="A timestamp marking when this transaction was initiated", format=Constants.TIMESTAMP_FORMAT)
    @JsonProperty
    @JsonFormat(shape=Shape.STRING, pattern=Constants.TIMESTAMP_FORMAT)
    private final Date dateTime;                         // datetime when the transaction began

    @JsonProperty
    @Schema(description="The type of message routed", allowableValues= {"submitSingleMessage", "connectivityTest", "ads"})
    private MessageType messageType = MessageType.INVALID_REQUEST; // Currently, this will always be later set to submitSingleMessage or connectivityTest

    /**
     * @deprecated
     */
    @JsonProperty
    @Schema(description="The thread identifier associated with the message")
    @Deprecated(since="2.0", forRemoval=true)
    private long threadId = 0;       // internal thread id that processed the transaction  NOSONAR

    @JsonProperty
    @Schema(description="The unique message id provided by the sender for this this transaction. Note: This field will have a random uuid unless set by the sender.")
    private String messageId = UUID.randomUUID().toString();

    @JsonProperty
    @Schema(description="The message id of the message this transaction was in response to")
    private String replyTo = "";                   // the ReplyTo attribute of the soap request

    /**
     * @deprecated
     */
    @JsonProperty
    @Schema(description="Additional identifiers associated with this transaction")
    @Deprecated(since="2.0", forRemoval=true)
    private String[] additionalIDs = {};           // Array list of additional IIS ids NOSONAR

    @JsonProperty
    @Schema(description="The type of payload associated with this transaction",
            allowableValues= {
            		"VXU", "QBP", "UNKNOWN",
                    "covidallMonthlyVaccination", "influenzaVaccination", "routineImmunization", "rsvPrevention"
            }
    )
    private RequestPayloadType requestPayloadType = RequestPayloadType.UNKNOWN;   // QBP, VXU, other - only avail if hl7 msg not hidden

    @JsonProperty
    @Schema(description="The size of the request payload")
    private int requestPayloadSize = 0;             // SOAP request body size in bytes

    @JsonProperty
    @Schema(description="The size of the response payload")
    private int responsePayloadSize = 0;            // SOAP response body size? - not collected yet

    @JsonProperty
    @Schema(description="Indicates if a response was received")
    private boolean responseReceived = false;      // Will be true unless there was a processing error

    @JsonProperty
    @Schema(description="The start time of the inbound request")
    private long startTime = 0;                   // Start time (msecs) at the gateway  to process the soap request

    @JsonProperty
    @Schema(description="The start time of the routed request")
    private long iisStartTime = 0;                   // Start time (msecs) at the IIS  to process the soap request

    @JsonProperty
    @Schema(description="The total elapsed time to complete the transaction")
    private long elapsedTimeTotal = 0;             // Total time (msecs) to process the soap request

    @JsonProperty
    @Schema(description="The elapsed time communicating with the destination")
    private long elapsedTimeIIS = 0;               // Time (msecs) taken to send request to IIS and receive a response

    @JsonProperty
    @Schema(description="The read time communicating with the destination")
    private long readTimeIIS = 0;			       // Time (msecs) taken to receive a response from the IIS

    @JsonProperty
    @Schema(description="The write time communicating with the destination")
    private long writeTimeIIS = 0;			       // Time (msecs) taken to send request to the IIS

    @JsonProperty
    @Schema(description="The elapsed time spent by IZ Gateway (overhead time)")
    private long elapsedTimeProcessing = 0;        // Time (msecs) spent processing in the gateway; not including waiting for IIS - doesn't include SSL process time

    @JsonProperty
    private String wsdlVersion = "2014";           // 2011 or 2014.

    @JsonProperty
    @Getter(AccessLevel.NONE)
    private boolean hasProcessError = false;       // True if there was any problem in processing the soap message

    @JsonProperty
    @Schema(description="The type of process error experienced during this transaction.")
    private String processErrorSummary = "";       // Summary of the process error.

    @JsonProperty
    @Schema(description="Details about process error experienced during this transaction.")
    private String processErrorDetail = "";           // Detail of the process error.

    @JsonProperty
    @Getter(AccessLevel.NONE)
    private boolean hasHL7Error = false;           // HL7 Error in the response from the IIS?

    @JsonProperty
    @Schema(description="The reported HL7 error")
    private String hl7Error = "";               // The HL7 error (up to max of 255 chars.)

    @JsonProperty
    @Schema(description="The request HL7 payload")
    private String requestHL7Message = "";          // SOAP request HL7 message

    @JsonProperty
    @Schema(description="The response HL7 Message")
    private String responseHL7Message = "";         // SOAP response HL7 message

    @JsonProperty
    @Schema(description="The cipher suite associated with the request (see source.cipherSuite)")
    private String cipherSuite = "";               // SOAP request cipher suite used

    @JsonProperty
    @Schema(description="The server mode", allowableValues= {"dev", "prod"})
    private String serverMode = AppProperties.isProduction() ? "prod" : "dev"; // Based on phiz.mode property, dev or prod

    @JsonProperty
    @Schema(description="Information about the source of the transaction")
    private SourceInfo source = new SourceInfo();

    @Getter // It has a special setter
    @JsonProperty
    @Schema(description="This is a known test message, only set in non-prod environments")
    private boolean knownTestMessage = false;
    
    @JsonProperty
    @Schema(description="The request MSH3 (Sending Application) value in the inbound message")
    @Getter
    private String requestMsh3 = null;  // Sending Application

    @JsonProperty
    @Schema(description="The request MSH4 (Sending Facility) value in the inbound message")
    @Getter
    private String requestMsh4 = null;  // Sending Facility

    @JsonProperty
    @Schema(description="The request MSH5 (Receiving Application) value in the inbound message")
    @Getter
    private String requestMsh5 = null;  // Receiving Application

    @JsonProperty
    @Schema(description="The request MSH6 (Receiving Facility) value in the inbound message")
    @Getter
    private String requestMsh6 = null;  // Receiving Facility

    @JsonProperty
    @Schema(description="The request MSH7 (Message Timestamp) value in the inbound message")
    @Getter
    private String requestMsh7 = null;  // TimeStamp

    @JsonProperty
    @Schema(description="The request MSH10 (Message Control ID) value in the inbound message")
    @Getter
    private String requestMsh10 = null; // MessageId

    @JsonProperty
    @Schema(description="The request MSH22 (Sending Organization) value in the inbound message")
    @Getter
    private String requestMsh22 = null; // Sending Organization

    @JsonProperty
    @Schema(description="The response MSH3 (Sending Application) value in the returned message")
    @Getter
    private String responseMsh3 = null;

    @JsonProperty
    @Schema(description="The response MSH4 (Sending Facility) value in the returned message")
    @Getter
    private String responseMsh4 = null;

    @JsonProperty
    @Schema(description="The response MSH5 (Receiving Application) value in the returned message")
    @Getter
    private String responseMsh5 = null;

    @JsonProperty
    @Schema(description="The response MSH6 (Receiving Facility) value in the returned message")
    @Getter
    private String responseMsh6 = null;

    @JsonProperty
    @Schema(description="The response MSH7 (Message Timestamp) value in the returned message")
    @Getter
    private String responseMsh7 = null;  // TimeStamp

    @JsonProperty
    @Schema(description="The response MSH10 (Message Control ID) value in the returned message")
    @Getter
    private String responseMsh10 = null; // MessageId

    @JsonProperty
    @Schema(description="The response MSH22 (Sending Organization) value in the returned message")
    @Getter
    private String responseMsh22 = null;

    @JsonProperty
    @Schema(description="The name of the fault that occured")
    private String faultName;

    @JsonProperty
    @Schema(description="The code identify the specific subtype of the fault that occured")
    private String faultCode;

    @JsonProperty
    @Schema(description="Information about the destination of this transaction")
    private DestinationInfo destination = new DestinationInfo();

    @JsonProperty
    /** Type of service: Gateway|Mock, used to differentiate different transactionData records */
    @Schema(description="The type of service requested", allowableValues= {"Mock", "Gateway"})
    private String serviceType;

    @JsonProperty
    @Schema(description="The size of the inbound SOAP Message")
    private long  inboundSoapMessageSize = 0;

    @JsonProperty
    @Schema(description="The size of the outbound SOAP Message")
    private long  outBoundSoapMessageSize = 0;

    @JsonProperty
    @Schema(description="The response to the request")
    private Object response;

    @JsonProperty
    @Schema(description="The number of attempts made to send the message")
    private int retries;

    // These next four track web services previous managed via LoggingInterceptor in 1.X
    @JsonProperty
    @Schema(description="Information about the request to IZ Gateway")
    @Getter
    private RequestInfo serverRequest = new RequestInfo();

    @JsonProperty
    @Schema(description="Information about the request to the destination")
    @Getter
    private RequestInfo clientRequest = new RequestInfo();

    @JsonProperty
    @Schema(description="Information about the response from the destination")
    @Getter
    private ResponseInfo clientResponse = new ResponseInfo();

    @JsonProperty
    @Schema(description="Information about the response from IZ Gateway")
    @Getter
    private ResponseInfo serverResponse = new ResponseInfo();

    public static String getNextEventId() {
        return EventId.INSTANCE.getNext();
    }

    public TransactionData() {
        this(new Date(), getNextEventId());
    }

    public TransactionData(String eventId) {
        this(new Date(), eventId);
    }

    public TransactionData(Date dt, String eventId) {
        if (StringUtils.isEmpty(eventId)) {
            eventId = getNextEventId();
        }
        this.transactionId = UUID.randomUUID().toString();
        this.threadId = Thread.currentThread().getId();  // NOSONAR
        this.dateTime = dt;
        this.startTime = dt.getTime();
        this.eventId = eventId.contains(".") ? eventId : (EventId.getPrefix() + "." + eventId);
    }

    /**
     * Limited copy constructor
     * @param that The object to make a partial copy of.
     */
    public TransactionData(TransactionData that) {
        this(that.dateTime, that.eventId);
        this.messageType = that.messageType;
        this.source = new SourceInfo(that.source);
        this.destination = new DestinationInfo(that.destination);
        this.serverMode = that.serverMode;
        this.serviceType = that.serviceType;
    }


    @JsonProperty
    @Schema(description="The IP Address of the source of this transaction (see source.ipAddress)")
    @Deprecated(since="2.0", forRemoval=true)
    public String getSourceIP() {  // NOSONAR Yep, it's deprecated
        return source.getIpAddress();
    }

    @JsonProperty
    @Schema(description="The Hostname of the source of this transaction (see source.host)")
    @Deprecated(since="2.0", forRemoval=true)
    public String getSourceHost() {	// NOSONAR Yep, it's deprecated
        return source.getHost();
    }

    @Schema(description="The type of the source of this transaction (see source.type)")
    @JsonProperty
    @Deprecated(since="2.0", forRemoval=true)
    public String getSourceType() {	// NOSONAR Yep, it's deprecated
        return source.getType();
    }

    @Schema(description="The facilityId of the source of this transaction (see source.facilityId)")
    @JsonProperty
    @Deprecated(since="2.0", forRemoval=true)
    public String getFacilityID() {	// NOSONAR Yep, it's deprecated
        return source.getFacilityId();
    }

    @Schema(description="The id of the destination for this transaction (see destination.id)")
    @JsonProperty
    @Deprecated(since="2.0", forRemoval=true)
    public String getDestinationID() {	// NOSONAR Yep, it's deprecated
        return destination.getId();
    }

    @Schema(description="The url of the destination of this transaction (see destination.url)")
    @JsonProperty
    @Deprecated(since="2.0", forRemoval=true)
    public String getDestinationURL() {	// NOSONAR Yep, it's deprecated
        return destination.getUrl();
    }

    @Schema(description="True if the transaction had an error during processing, false if it succeeded")
    @JsonProperty
    public boolean getHasProcessError() {
        return hasProcessError;
    }
    @Schema(description="True if the transaction reported an error in the HL7 response message")
    @JsonProperty
    public boolean getHasHL7Error() {
        return hasHL7Error;
    }
    @JsonProperty
    @Schema(description="The full process error (Summary: Description format)")
    public String getProcessError() {
        if (StringUtils.isEmpty(processErrorDetail)) {
        	return processErrorSummary;
        }
        return processErrorSummary + ": " + processErrorDetail;
    }

    @JsonProperty
    public Object getResponse() {
        if (response != null) {
            return response;
        }
        if (!StringUtils.isEmpty(responseHL7Message)) {
            Map<String, Object> m = new TreeMap<>();
            m.put("HL7", responseHL7Message);
            return m;
        }
        return null;
    }
    public void setRequestEchoBack(String val) {
        requestHL7Message = val;
        setKnownTestMessage(true);  // Echo messages are test messages.
        setRequestPayloadType(RequestPayloadType.OTHER);
        setRequestPayloadSize(StringUtils.length(val));
    }
    public void setResponseEchoBack(String val) {
        responseHL7Message = val;
        setResponsePayloadSize(StringUtils.length(val));
    }
    
    /**
     * Indicates if the message matches known test patterns
     * @param message	The message
     * @return true if the message is for a known test case
     */
    public boolean matchesTest(String message) {
    	if (message == null) {
    		return true;
    	}
        String[] segments = message.split("[\r\n]");
        String[] mshParts = segments[0].split("\\|");
        String msgType = getFirstFieldComponent(mshParts, HL7Message.MESSAGE_TYPE - 1);
        int segIndex = 0;
        int found = 0;
        int fieldLoc = 4;
        if (msgType.contains("QBP")) {
        	for (segIndex = 1; segIndex < segments.length; segIndex ++) {
        		if (segments[segIndex].startsWith("QPD")) {
        			found = segIndex;
        			fieldLoc = 4;
        			break;
        		}
        	}
        } else {
        	for (segIndex = 1; segIndex < segments.length; segIndex ++) {
        		if (segments[segIndex].startsWith("PID")) {
        			found = segIndex;
        			fieldLoc = 5;
        			break;
        		}
        	}
        }
        if (found == 0) {
        	// NO PID or QPD segment, matches test patterns for MOCK, or for Error cases.
        	return true;
        }
        String[] fields = segments[found].split("\\|");
        return fields.length > fieldLoc && isKnownTestPatient(mshParts, fields[fieldLoc]);
    }
    
    /**
     * Determines if this is a patient matching a KNOWN test pattern. 
     * @param mshParts 
     * @param name	Patient name in the message
     * @return	true if the patient matches the pattern
     */
    private boolean isKnownTestPatient(String[] mshParts, String name) {
    	if (name == null) {
    		return false;
    	}
    	String[] nameParts = name.toUpperCase().split("\\^");
    	String familyName = nameParts[0];
    	String givenName = nameParts.length > 1 ? nameParts[1] : "";
    	
    	if (familyName.isEmpty() && givenName.isEmpty()) {
    		// QPD w/o name parts is OK.
    		return true;
    	}
    	if (familyName.contains("IZG") || givenName.contains("IZG")) {
    		return true;
    	}
    	if (familyName.endsWith("AIRA") && givenName.endsWith("AIRA")) {
    		return true;
    	}
    	if (familyName.startsWith("DOCKET") && givenName.startsWith("DOCKET")) {
    		return true;
    	}
    	if (familyName.startsWith("ZZ") && givenName.startsWith("ZZ")) {
    		return true;
    	}
  	
    	if (familyName.endsWith("TEST") && givenName.endsWith("TEST")) {
    		return true;
    	}

    	// Specifically marked as a test message in MSH-5.
    	return (mshParts.length <= 5 || mshParts[5].equals("TEST") || mshParts[3].equals("TEST"));
	}

	  /**
     * Mark this message as a known test message if true, otherwise treat as if it contains PHI
     * @param test true for known test messages, false otherwise
     * @return the set value.
     */
    public boolean setKnownTestMessage(boolean test) {
    	this.knownTestMessage = test;
    	return this.knownTestMessage;
    }
    
    public void setRequestHL7Message(String val) {
        requestHL7Message = isProd() || !setKnownTestMessage(matchesTest(val))  ? HL7Utils.protectHL7Message(val) : val;
        if (!StringUtils.isEmpty(requestHL7Message)) {
            String[] mshParts = StringUtils.substring(requestHL7Message,
                    0, StringUtils.indexOfAny(requestHL7Message, HL7Message.SEGMENT_SEPARATORS)
            ).split("\\|");
            requestMsh3 = getFirstFieldComponent(mshParts, HL7Message.SENDING_APPLICATION - 1);
            requestMsh4 = getFirstFieldComponent(mshParts, HL7Message.SENDING_FACILITY - 1);
            requestMsh5 = getFirstFieldComponent(mshParts, HL7Message.RECEIVING_APPLICATION - 1);
            requestMsh6 = getFirstFieldComponent(mshParts, HL7Message.RECEIVING_FACILITY - 1);
            requestMsh7 = getField(mshParts, HL7Message.MESSAGE_DATETIME - 1);
            String msgType = getFirstFieldComponent(mshParts, HL7Message.MESSAGE_TYPE - 1);
            requestPayloadType = RequestPayloadType.fromString(msgType);
            requestMsh10 = getField(mshParts, HL7Message.MESSAGE_CONTROL_ID - 1);
            requestMsh22 = getFirstFieldComponent(mshParts, HL7Message.SENDING_RESPONSIBLE_ORGANIZATION - 1);
        }
        setRequestPayloadSize(StringUtils.length(val));
    }

    public void setResponseHL7Message(String val) {
    	if (isKnownTestMessage() && !matchesTest(val)) {
    		// Response should match test message requirements as well.  If it doesn't
    		// reset test to false.
    		setKnownTestMessage(false);
    	}
        responseHL7Message = isProd() || !isKnownTestMessage() ? HL7Utils.protectHL7Message(val)  : val;
        if (!StringUtils.isEmpty(responseHL7Message)) {
            String[] mshParts = StringUtils.substring(responseHL7Message,
                    0, StringUtils.indexOfAny(requestHL7Message, HL7Message.SEGMENT_SEPARATORS)
            ).split("\\|");
            responseMsh3 = getFirstFieldComponent(mshParts, HL7Message.SENDING_APPLICATION - 1);
            responseMsh4 = getFirstFieldComponent(mshParts, HL7Message.SENDING_FACILITY - 1);
            responseMsh5 = getFirstFieldComponent(mshParts, HL7Message.RECEIVING_APPLICATION - 1);
            responseMsh6 = getFirstFieldComponent(mshParts, HL7Message.RECEIVING_FACILITY - 1);
            responseMsh7 = getField(mshParts, HL7Message.MESSAGE_DATETIME - 1);
            responseMsh10 = getField(mshParts, HL7Message.MESSAGE_CONTROL_ID - 1);
            responseMsh22 = getFirstFieldComponent(mshParts, HL7Message.SENDING_RESPONSIBLE_ORGANIZATION - 1);
        }
        setResponsePayloadSize(StringUtils.length(val));
        if (!StringUtils.isEmpty(val) && HL7_ERROR_PATTERN.matcher(val).find()) {
            // If the given string is not empty and  has an error token in it, then update the hl7 error message in the object.
            // (Also set the boolean appropriately)
            // Note: if this string has no error token, don't mess with existing values as there may already be an error present from other strings.
            hasHL7Error = true;
            hl7Error = responseHL7Message;
        } else {
            hasHL7Error = false;
            hl7Error = "";
        }
    }

    @JsonIgnore
    private String getField(String[] parts, int index) {
        return parts.length > index ? parts[index] : null;
    }

    @JsonIgnore
    private String getFirstFieldComponent(String[] parts, int index) {
        return StringUtils.substringBefore(getField(parts, index),"^");
    }

    public void setProcessError(String summary, String detail) {
        processErrorSummary = HL7Utils.maskSegments(summary);
        processErrorDetail = HL7Utils.maskSegments(detail);
        hasProcessError = !StringUtils.isAllEmpty(summary, detail);
    }
    
    
	public void setProcessError(Exception fault) {
        FaultSupport s = null;
		if (fault instanceof UnsupportedOperationFault f) {
			setMessageType(MessageType.INVALID_REQUEST);
			s = f;
		} else if (fault instanceof Fault f) {
            s = f;
            if (f instanceof HubClientFault hcf && hcf.getOriginalBody() != null) {
            	this.setResponse(hcf.getOriginalBody());
            }
        } else {
            s = new UnexpectedExceptionFault(fault, null);
        }
        setProcessError(s.getSummary(), s.getDetail());
        setFaultName(s.getFaultName());
        setFaultCode(s.getCode());
    }

    @JsonProperty
    public boolean isProd() {
        return !"dev".equals(serverMode);
    }

    @Override
    public String toString() {
    	return getMessage();
    }

    @JsonIgnore
    public String getMessage() {
        String unknown = "**unknown**";
        return String.format("%sTransaction %s(%s) containing %s(%s) "
                        + "from %s at %s "
                        + "to %s at %s(%s) "
                        + "%s%s",
                "Mock".equalsIgnoreCase(getServiceType()) ? "Mock handled " : "",
                getRequestMsh10() == null ? getTransactionId() : getRequestMsh10(),
                getEventId(),
                getMessageType(),
                getRequestPayloadType(),
                source == null ? unknown : source.getName(),
                source == null ? unknown : source.getIpAddress(),
                destination == null ? unknown : destination.getName(),
                destination == null ? unknown : destination.getIpAddress(),
                destination == null ? unknown : destination.getUrl(),
                hasProcessError ? "failed" : "succeeded",
                hasProcessError ? " with error: " + getProcessError(): ""
        );
    }

    public void computeTransactionTimes() {
        setElapsedTimeTotal(System.currentTimeMillis() - getStartTime());
        setElapsedTimeProcessing(getElapsedTimeTotal() - getElapsedTimeIIS());
        setWriteTimeIIS(getElapsedTimeIIS() - getReadTimeIIS());
    }

    public void logIt() {
        computeTransactionTimes();
        log.info(Markers2.append("transactionData", this), "{}", getMessage());
    }
}
