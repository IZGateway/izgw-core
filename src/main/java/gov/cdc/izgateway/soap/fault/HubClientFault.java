package gov.cdc.izgateway.soap.fault;

import gov.cdc.izgateway.common.HasDestinationUri;
import gov.cdc.izgateway.model.IDestination;
import gov.cdc.izgateway.model.RetryStrategy;
import gov.cdc.izgateway.soap.message.FaultMessage;
import gov.cdc.izgateway.soap.message.SoapMessage;
import gov.cdc.izgateway.utils.HL7Utils;
import gov.cdc.izgateway.utils.XmlUtils;
import lombok.Getter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The HubClientFault class is used to report errors when the client reports a SOAP Fault or other connection error.  
 * 
 * @author Audacious Inquiry
 */
public class HubClientFault extends Fault implements HasDestinationUri {

	private static final long serialVersionUID = 1L;
	/** The name of the fault */
	public static final String FAULT_NAME = "HubClientFault";
	private static final String FAULT = "Fault";
	private static final MessageSupport[] MESSAGE_TEMPLATES = { new MessageSupport(FAULT_NAME, "220",
			"Certificate Error", null,
			"The server certificate presented by the ADS destination is not trusted by IZ Gateway. "
					+ "It may have expired, been revoked, or the certificate authority may not be trusted by IZ Gateway. "
					+ "Contact IZ Gateway support.",
			RetryStrategy.CONTACT_SUPPORT),

			new MessageSupport(FAULT_NAME, "221", "Certificate Error", null,
					"The server certificate presented by the IIS is not trusted by IZ Gateway. "
							+ "It may have expired, been revoked, or the certificate authority may not be trusted by IZ Gateway. Retry after "
							+ "verifying that the certificate has been updated by the IIS , or contact IZ Gateway support "
							+ "if the certificate chain is not trusted.",
					RetryStrategy.CHECK_IIS_STATUS),
			new MessageSupport(FAULT_NAME, "222", "Not Presently Used", null, "This shouldn't happen.",
					RetryStrategy.CONTACT_SUPPORT),
			new MessageSupport(FAULT_NAME, "223", "Cannot Call IIS Destination", null,
					"The destination could not be reached.", RetryStrategy.CONTACT_SUPPORT),

			new MessageSupport(FAULT_NAME, "224", "Destination Threw MessageTooLargeFault", null,
					"The message being sent is too large for the destination to process.",
					RetryStrategy.CORRECT_MESSAGE),
			new MessageSupport(FAULT_NAME, "225", "Destination Threw SecurityFault", null,
					"The destination rejected the message because either the facilityId is incorrect, one of the MSH values representing the sender is incorrect, "
							+ "or IZ Gateway is configured with the wrong username and password. Contact the jurisdiction to verify the correct values to use "
							+ "for facilityId and in MSH headers and correct the problem if they are not. If the values being sent are correct, "
							+ "contact support",
					RetryStrategy.CORRECT_MESSAGE),
			new MessageSupport(FAULT_NAME, "226", "Destination Threw UnsupportedOperationFault", null,
					"The destination does not support this SOAP message type", RetryStrategy.CONTACT_SUPPORT),
			new MessageSupport(FAULT_NAME, "227", "Destination Threw Fault", null,
					"The destination returned a generic fault. See the fault details.", RetryStrategy.CONTACT_SUPPORT),
			new MessageSupport(FAULT_NAME, "228", "Destination Returned Invalid Response", null,
					"The destination returned an invalid response to the message. Please contact the destination IIS.", RetryStrategy.CHECK_IIS_STATUS),

			new MessageSupport(FAULT_NAME, "201", "HTTP Bad Request Error", null,
					"The Destination sent a 'Bad Request' HTTP Error code in response to the request. "
							+ "This is likely due to either an error in the inbound SOAP Message or in the integration at the IIS side.",
					RetryStrategy.CHECK_IIS_STATUS, Arrays.asList(HttpStatus.BAD_REQUEST)),

			new MessageSupport(FAULT_NAME, "202", "HTTP Access Control Error", null,
					"The Destination sent an HTTP Error code indicating an access control failure in response to the request. "
							+ "This may be due to invalid credentials presented IZ Gateway or an invalid facility id presented in "
							+ "the HL7 Message by the sender. Verify the facility id with the destination IIS and if that fails,"
							+ "contact IZ Gateway support.",
					RetryStrategy.CORRECT_MESSAGE, Arrays.asList(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN)),

			new MessageSupport(FAULT_NAME, "203", "HTTP Request Timeout Error", null,
					"The Destination endpoint reported a request timeout. "
							+ "This can be caused by a backlog of requests at the destination. Use the normal retry strategy.",
					RetryStrategy.NORMAL, Arrays.asList(HttpStatus.REQUEST_TIMEOUT)),

			new MessageSupport(FAULT_NAME, "204", "HTTP Not Found Error", null,
					"The Destination endpoint reported that the requested endpoint or resource was not found. "
							+ "The host server at the destination is running, but the endpoint url or resource is not known. This can occur during maintenance, "
							+ "or may also occur when there is a problem at the destination.",
					RetryStrategy.CHECK_IIS_STATUS, Arrays.asList(HttpStatus.NOT_FOUND)),

			new MessageSupport(FAULT_NAME, "205", "HTTP Internal Server Error", null,
					"The Destination reported an internal server HTTP Error code in response to the request. "
							+ "The destination may be offline for maintenance.",
					RetryStrategy.CHECK_IIS_STATUS, Arrays.asList(HttpStatus.INTERNAL_SERVER_ERROR)),
			new MessageSupport(FAULT_NAME, "206", "HTTP Gateway Error", null,
					"The Destination sent an internal infrastructure HTTP Error code in response to the request. "
							+ "This error code usually indicates that a destination component is offline for maintenance.",
					RetryStrategy.CHECK_IIS_STATUS,
					Arrays.asList(HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT, HttpStatus.INTERNAL_SERVER_ERROR)),
			new MessageSupport(FAULT_NAME, "207", "HTTP Unknown Error", null,
					"The Destination sent an unexpected HTTP Error code. "
							+ "This error code do not make sense in the context of the IZ Gateway integration, and may be a result of "
							+ "misconfiguration in the destination IIS.",
					RetryStrategy.CONTACT_SUPPORT, Collections.emptyList()) };
	
	private static final List<String> retryableErrors = Arrays.asList("203", "204", "205", "206", "207");
	@Override
	public boolean isRetryable() {
		return retryableErrors.contains(getCode());
	}

	@Override
	public boolean shouldBreakCircuit() {
		return isRetryable();
	}
	
	private static final Map<Integer, MessageSupport> statusToMessageMap = new HashMap<>();
	static {
		for (MessageSupport m : MESSAGE_TEMPLATES) {
			MessageSupport.registerMessageSupport(m);

			Object extra = m.getExtra();
			if (extra instanceof List) {
				List<?> stati = (List<?>) extra;
				for (Object status : stati) {
					if (status instanceof HttpStatus s) {
						statusToMessageMap.put(s.value(), m);
					}
				}
				// if the list is an empty one, then use it for any unresolved status values.
				if (stati.isEmpty()) {
					for (HttpStatus st : HttpStatus.values()) {
						if (statusToMessageMap.get(st.value()) == null) {
							statusToMessageMap.put(st.value(), m);
						}
					}
					break;
				}
			}
		}
	}

	private HubClientFault(MessageSupport messageSupport, IDestination destination, Throwable rootCause, int statusCode,
			String originalBody, SoapMessage faultMessage) {
		super(messageSupport, rootCause);
		this.destination = destination;
		this.originalBody = HL7Utils.maskSegments(originalBody);
		this.faultMessage = faultMessage;
		this.statusCode = statusCode;
	}

	/** 
	 * Client returned something, but it didn't parse, go figure it out
	 * @param rootCause	The root cause of the error
	 * @param dest	The destination
	 * @param statusCode	The status code of the response
	 * @param body	The message body
	 * @return The hub client fault
	 */
	public static HubClientFault invalidMessage(Throwable rootCause, IDestination dest, int statusCode, InputStream body) {
		return invalidMessage(rootCause, dest, statusCode, null, body);
	}
	/** 
	 * Client returned something, but it didn't parse, go figure it out
	 * @param rootCause	The root cause of the error
	 * @param dest	The destination
	 * @param statusCode	The status code of the response
	 * @param path The path being accessed
	 * 
	 * @param body	The message body
	 * @return The hub client fault
	 */
	public static HubClientFault invalidMessage(Throwable rootCause, IDestination dest, int statusCode, String path, InputStream body) {
		String bodyString = XmlUtils.toString(body);
		if (statusCode != 500 && statusCode != 200) {
			return new HubClientFault(getHttpMessageSupport(statusCode, path), dest, rootCause, statusCode, bodyString, null);
		}
		if (statusCode == 200) {
			// These are dangerous. The client thought it had successfully shipped something, but it wasn't valid.
			// DO NOT REPORT the original response as it may have a corrupted HL7 message containing PHI.
			bodyString = null; 
		}
		while (rootCause.getCause() != null) {
			rootCause = rootCause.getCause();
		}
		return new HubClientFault(MESSAGE_TEMPLATES[8].setDetail(rootCause.getMessage()), dest, rootCause, statusCode, bodyString, null);
	}

	// Client returns 500 (or 400) with a fault message
	/**
	 * Construct a HubClientFault  
	 * @param rootCause	The root cause of the fault
	 * @param dest	The destination throwing the fault
	 * @param statusCode	The status code returned 
	 * @param body	The body of the fault content
	 * @param result	The resulting soap message
	 * @param path 
	 * @return	The hub client fault
	 */
	public static HubClientFault clientThrewFault(Throwable rootCause, IDestination dest, int statusCode,
			InputStream body, SoapMessage result, String path) {
		String bodyString = XmlUtils.toString(body);
		return new HubClientFault(HubClientFault.getMessageSupport(rootCause, result, bodyString, statusCode, dest, path),
				dest, rootCause, statusCode, bodyString, result);
	}

	private static final String FAULT_XML = 
			"<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'><soap:Fault>"
				    + "<soap:Code>"
				    + "<soap:Value>soap:Receiver</soap:Value></soap:Code>"
				    + "<soap:Reason><soap:Text>Invalid Username, Password or FacilityID</soap:Text></soap:Reason>" 
				    + "<soap:Detail>"
				    + "<ns3:SecurityFault xmlns:ns3='urn:cdc:iisb:2011'>" 
				    + "<ns3:Code>401</ns3:Code>"
				    + "<ns3:Reason>Security Fault</ns3:Reason>" // Change from required "Security" to "Security Fault"
				    + "<ns3:Detail>Invalid Username, Password or FacilityID</ns3:Detail>"
				    + "</ns3:SecurityFault>"
				    + "</soap:Detail></soap:Fault></soap:Envelope>";

	/**
	 * Construct a developer selected fault for error response testing
	 * @param dest	The destination throwing the fault
	 * @return	The Hub Client Fault
	 */
	public static HubClientFault devAction(IDestination dest) {
		return clientThrewFault(UnsupportedOperationFault.devAction(), dest, 420,
				new ByteArrayInputStream(FAULT_XML.getBytes(StandardCharsets.UTF_8)),
				new SoapMessage(), null);
	}

	/**
	 * Construct a HubClientFault when the Client returns an Http status code without a valid fault message. 
	 * @param dest	The destination 
	 * @param statusCode	The status code
	 * @param error	The error message
	 * @param path 
	 * @return	The hub client fault
	 */
	public static HubClientFault httpError(IDestination dest, int statusCode, String error, String path) {
		return new HubClientFault(getHttpMessageSupport(statusCode, path), dest, null, statusCode, error, null);
	}

	/**
	 * Create a generic simulated fault
	 * @param dest	The destination
	 * @return	The simulated fault
	 */
	public static HubClientFault devHttpAction(IDestination dest) {
		return httpError(dest, 420, "This is a simulated fault", dest.getDestUri());
	}

	/**
	 * Get a MessageSupport object appropriate to the exception thrown.
	 * 
	 * @param rootCause     The exception thrown.
	 * @param faultMessage
	 * @param originalBody2 An XML Document reflecting any original error reported
	 *                      by the destination.
	 * @param statusCode2 
	 * @return A MessageSupport object appropriate to the exception thrown
	 */
	private static MessageSupport getMessageSupport(Throwable rootCause, SoapMessage faultMessage, String originalBody2,
			int statusCode2, IDestination destination, String path) {
		String[] details = { null };
		String faultName = null;
		if (faultMessage instanceof FaultMessage fm && !FAULT.equals(fm.getFaultName())) {
			faultName = fm.getFaultName();
			details[0] = fm.getReason();
			
		} else {
			faultName = getFaultName(rootCause, originalBody2, details);
			if (details[0] == null) {
				details[0] = rootCause == null ? null : rootCause.getMessage();
			}
		}
		if (StringUtils.isEmpty(faultName)) {
			return new MessageSupport(FAULT_NAME, "223", details[0]);
		}
		switch (faultName) {
		case "CertificateException":
			if (destination.isDex()) {
				return new MessageSupport(FAULT_NAME, "220", rootCause.getMessage());
			}
			return new MessageSupport(FAULT_NAME, "221", rootCause.getMessage());

		case "MessageTooLargeFault":
			return new MessageSupport(FAULT_NAME, "224", details[0]);
		case "SecurityFault":
			return new MessageSupport(FAULT_NAME, "225", details[0]);
		case "UnsupportedOperationFault":
			return new MessageSupport(FAULT_NAME, "226", details[0]);
		case FAULT:
			return new MessageSupport(FAULT_NAME, "227", details[0]);
		case "NoSuchElementException":
			return new MessageSupport(FAULT_NAME, "228", details[0]);
		default:
			if (statusCode2 != 500) {
            	return getHttpMessageSupport(statusCode2, path);
			}
			return new MessageSupport(FAULT_NAME, "223", details[0]);
		}
	}

	/**
	 * Get a MessageSupport object appropriate to the exception thrown.
	 *
	 * @return A MessageSupport object appropriate to the exception thrown
	 */
	private static MessageSupport getHttpMessageSupport(int statusCode, String path) {
		HttpStatus status = HttpStatus.resolve(statusCode);
		if (status == null) {
			return MESSAGE_TEMPLATES[MESSAGE_TEMPLATES.length - 1];
		}
		return statusToMessageMap.get(status.value()).setSummary(String.format("HTTP Error %d", statusCode),
				status.getReasonPhrase() + (StringUtils.isNotEmpty(path) ? " accessing " + path : ""));
	}

	private static String getFaultName(Throwable rootCause, String originalBody, String[] details) {
		String faultName = rootCause == null ? "Fault" : rootCause.getClass().getSimpleName();

		if (originalBody == null) {
			return faultName;
		}
		
		Document doc = XmlUtils.parseDocument(originalBody);
		// This results from a validation error in the fault that was returned by the
		// destination.
		if (firstChildIsFault(doc) || documentElementIsError(doc)) {
			Element xmlName = getFaultName(doc.getDocumentElement());
			if (xmlName != null) {
				details[0] = getDetail(doc.getDocumentElement());
				return xmlName.getLocalName();
			} else {
				details[0] = getErrorMessage(doc.getDocumentElement());
			}
		}
		return faultName;
	}

	private static boolean documentElementIsError(Document originalError) {
		return "ErrorText".equals(originalError.getFirstChild().getNodeName());
	}

	private static String getDetail(Element documentElement) {
		Element fault = getElement(documentElement, "Reason");
		if (fault != null) {
			Element text = getElement(fault, "Text");
			Node child = text != null ? text.getFirstChild() : fault.getFirstChild();
			if (XmlUtils.isTextNode(child)) {
				return child.getNodeValue();
			}
		}
		return null;
	}

	private static String getErrorMessage(Element documentElement) {
		NodeList nl = documentElement.getElementsByTagName("Message");
		if (nl.getLength() > 0) {
			Node child = nl.item(0).getFirstChild();
			if (XmlUtils.isTextNode(child)) {
				return child.getNodeValue();
			}
		}
		return null;
	}

	private static Element getFaultName(Element originalError) {
		Element fault = getElement(originalError, FAULT);
		if (fault != null) {
			fault = getElement(fault, "Detail");
			if (fault != null) {
				// First detail element should be fault name
				fault = getElement(fault, "*");
				if (fault != null) {
					String localName = fault.getLocalName();
					if (localName.endsWith(FAULT)) {
						return fault;
					}
				}
			}
		}
		return null;
	}

	private static boolean firstChildIsFault(Document originalError) {
		return getElement(originalError.getDocumentElement(), FAULT) != null;
	}

	private static Element getElement(Element p, String name) {
		NodeList l = p.getElementsByTagNameNS("*", name);
		if (l.getLength() > 0) {
			return (Element) l.item(0);
		}
		return null;
	}

	@Getter
	private final SoapMessage faultMessage;
	@Getter
	private final IDestination destination;
	@Getter
	private final int statusCode;
	@Getter
	private final String originalBody;

	@Override
	public String getDestinationId() {
		return destination == null ? null : destination.getDestId();
	}

	@Override
	public String getDestinationUri() {
		return destination == null ? null : destination.getDestUri();
	}

}
