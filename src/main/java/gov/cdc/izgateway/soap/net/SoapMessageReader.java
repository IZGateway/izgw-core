package gov.cdc.izgateway.soap.net;

import java.util.ArrayDeque;

import java.util.Deque;
import java.util.function.Consumer;
import java.util.function.Supplier;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamLocation2;

import gov.cdc.izgateway.common.BadRequestException;
import gov.cdc.izgateway.configuration.AppProperties;
import gov.cdc.izgateway.logging.info.EndPointInfo;
import gov.cdc.izgateway.logging.markers.Markers2;
import gov.cdc.izgateway.soap.fault.SecurityFault;
import gov.cdc.izgateway.soap.message.ConnectivityTestRequest;
import gov.cdc.izgateway.soap.message.ConnectivityTestResponse;
import gov.cdc.izgateway.soap.message.FaultMessage;
import gov.cdc.izgateway.soap.message.HasCredentials;
import gov.cdc.izgateway.soap.message.HasEchoBack;
import gov.cdc.izgateway.soap.message.HasFacilityID;
import gov.cdc.izgateway.soap.message.HasHL7Message;
import gov.cdc.izgateway.soap.message.SoapMessage;
import gov.cdc.izgateway.soap.message.SubmitSingleMessageRequest;
import gov.cdc.izgateway.soap.message.SubmitSingleMessageResponse;
import gov.cdc.izgateway.utils.XmlUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

@Slf4j
public class SoapMessageReader {
	private static final String TAG_NAME_PATTERN = "script";
	private static final String TEXT_VALUE_PATTERN = "javascript";
	/**
	 * The type of messages to parser, Inbound Request or Inbound Response
	 */
	@Getter
	private final String type;

	/**
	 * The reader used to process the XML input
	 */
	@Getter
	@Setter
	private XMLStreamReader reader;

	/**
	 * The sender of the input.
	 */
	@Getter
	@Setter
	private EndPointInfo endpoint;

	/**
	 * True if reading from hub. 
	 */
	@Getter
	@Setter
	private boolean isHub = false;

	/**
	 * The last element seen in context.
	 */
	@Getter
	private String lastElement;

	private Deque<QName> stack;
	private SoapMessage req;
	private HasEchoBack eb;
	private HasHL7Message hl7;
	private HasFacilityID fac;
	private HasCredentials cred;
	private FaultMessage fm;
	private String hubHeader;
	private boolean isHtmlMessage = false;
	private String documentElementName = null;

	/** You can set a writer to copy events to on the reader. */
	private XMLStreamWriter writer;
	
	/**
	 * Wraps exceptions found during parsing
	 * 
	 * @author Audacious Inquiry
	 */
	public static class SoapParseException extends XMLStreamException {
		private static final long serialVersionUID = 1L;
		@Getter
		private final String exchange;
		SoapParseException(Throwable th, String exchange) {
			super(getMessage(th), 
				th instanceof XMLStreamException xex ? xex.getLocation() : XMLStreamLocation2.NOT_AVAILABLE, th);
			this.exchange = exchange;
		}
		/**
		 * Remove extra text in the produced message.
		 * @param th	The original exception.
		 * @return	Just the message without location and Message: prefix
		 */
		private static String getMessage(Throwable th) {
			if (th instanceof XMLStreamException) {
				String msg = StringUtils.substringAfter(th.getMessage(), "Message: ");
				if (StringUtils.isBlank(msg)) {
					msg = th.getMessage();
				}
				return msg;
			}
			return th.getMessage();
		}
	}
	/**
	 * Read a soap message
	 * @param reader	The XML Reader to use
	 * @param type		The schema associated with the message
	 * @param writer	The writer to use to preserve the original XML, or null to skip preservation. 
	 */
	public SoapMessageReader(XMLStreamReader reader, String type, XMLStreamWriter writer) {
		reset();
		this.reader = reader;
		this.type = type;
		this.writer = writer;
	}

	public SoapMessageReader(XMLStreamReader reader, String type) {
		this(reader, type, null);
	}
	/**
	 * Reset the reader for the next time through.
	 */
	public void reset() {
		reader = null;
		stack = new ArrayDeque<>();
		req = new SoapMessage();
		eb = null;
		hl7 = null;
		fac = null;
		cred = null;
		lastElement = null;
		endpoint = null;
		hubHeader = null;
		isHtmlMessage = false;
		documentElementName = null;
	}

	/**
	 * Translate the SoapMessages for IZ Gateway Hub, CDC-2011 and 2014 WDSLs into
	 * business objects containing the relevant information.
	 * 
	 * @return A SoapMessage of the appropriate type representing the message.
	 * @throws SecurityFault
	 * @throws XMLStreamException
	 */
	public SoapMessage read() throws SecurityFault, XMLStreamException {
		try {
			boolean filter = AppProperties.isProduction();
			int eventType;
			while ((eventType = reader.next()) != XMLStreamConstants.END_DOCUMENT) {
				boolean skip = false;
				switch (eventType) {
				case XMLStreamConstants.START_ELEMENT:
					skip = parseElement(filter);
					QName element = reader.getName();
					stack.push(element);
					lastElement = toNameString(element);
					break;
				case XMLStreamConstants.END_ELEMENT:
					stack.pop();
					lastElement = toNameString(stack.peekFirst());
					break;
				case XMLStreamConstants.CDATA, XMLStreamConstants.CHARACTERS:
					validateText(reader.getText());
					break;
				default:
					break;
				}

				// If we are saving output, and shouldn't skip handling of this element
				// content.
				if (!skip) {
					copyEvent();
				}
			}
		} catch (XMLStreamException ex) {  // NOSONAR Exception handling is OK
			log.error(Markers2.append(ex), "XMLStreamException parsing SOAP Message");
			throw new SoapParseException(ex, req == null ? null : req.getClass().getSimpleName());
		} catch (BadRequestException ex) {	// NOSONAR Exception handling is OK
			log.error(Markers2.append(ex), "Invalid XML input in SOAP Message");
			throw new SoapParseException(ex, req == null ? null : req.getClass().getSimpleName());
		} catch (SecurityFault sf) {
			throw sf;
		} catch (Exception ex) {	// NOSONAR Exception handling is OK
			log.error(Markers2.append(ex), "Unexpected exception parsing SOAP Message");
			throw new SoapParseException(ex, req == null ? null : req.getClass().getSimpleName());
		}
		req.updateAction(req.isHubMessage());
		return req;
	}

	private void copyEvent() {
		if (writer == null) {
			return;
		}
		try {
			// Copy the event, ignoring any whitespace nodes.
			XmlUtils.copyEvent(reader, writer, true);
		} catch (XMLStreamException ignoreThis) {
			// Got an exception on the writer, we will ignore it
			// and stop processing writer events.
			writer = null;
		}
	}

	/**
	 * Parse a single element
	 * @param filter	True if certain content needs to be filtered
	 * @return True if text was also read from after the element
	 * @throws XMLStreamException
	 * @throws SecurityFault
	 */
	private boolean parseElement(boolean filter) throws XMLStreamException, SecurityFault {
		String name = reader.getName().toString();
		switch (name) {  // NOSONAR -- Yes, this is a big switch
		case "{urn:cdc:iisb:hub:2014}HubRequestHeader", "{urn:cdc:iisb:hub:2014}HubResponseHeader":
			hubHeader = name;
			verifyAttributes();
			return false;
		case "{urn:cdc:iisb:hub:2014}DestinationId":
			verifyAndSet(hubHeader, null, req.getHubHeader()::setDestinationId);
			return true;
		case "{urn:cdc:iisb:hub:2014}DestinationUri":
			verifyAndSet(hubHeader, null, req.getHubHeader()::setDestinationUri);
			return true;
		
		// Process wsa headers.
		// For the following cases, IZGW will set WSA headers to the last value specified
		// rather than enforcing the rule that only one of each may be present.  These
		// headers are NOT essential for IZGW processing.
		case "{http://www.w3.org/2005/08/addressing}Action":
			verifyAndSet(req, null, req.getWsaHeaders()::setAction);
			return true;
		case "{http://www.w3.org/2005/08/addressing}MessageID":
			verifyAndSet(req, null, req.getWsaHeaders()::setMessageID);
			return true;
		case "{http://www.w3.org/2005/08/addressing}To":
			verifyAndSet(req, null, req.getWsaHeaders()::setTo);
			return true;
		case "{http://www.w3.org/2005/08/addressing}RelatesTo":
			verifyAndSet(req, null, req.getWsaHeaders()::setRelatesTo);
			return true;
		case "{http://www.w3.org/2005/08/addressing}From":
			verifyAndSet(req, null, req.getWsaHeaders()::setFrom);
			return true;
		// End of wsa headers
			
		case "{urn:cdc:iisb:2011}connectivityTest", "{urn:cdc:iisb:2014}ConnectivityTestRequest":
			eb = updateSoapMessageType(req, ConnectivityTestRequest.class, reader.getNamespaceURI());
			req = (SoapMessage) (eb);
			cred = (HasCredentials) req;
			verifyAttributes();
			return false;

		case "{urn:cdc:iisb:2011}connectivityTestResponse", "{urn:cdc:iisb:2014}ConnectivityTestResponse":
			eb = updateSoapMessageType(req, ConnectivityTestResponse.class, reader.getNamespaceURI());
			req = (SoapMessage) (eb);
			verifyAttributes();
			return false;

		case "{urn:cdc:iisb:2011}return":
			if (eb != null) {
				verifyAndSet(eb, eb::getEchoBack, eb::setEchoBack);
			} else if (hl7 != null) {
				int flags = verifyAndSet(hl7, hl7::getHl7Message, hl7::setHl7Message, filter);
				if (hasCData(flags)) {
					hl7.setCdataWrapped(true);
				}
			}
			return true;
		case "{urn:cdc:iisb:2011}echoBack", "{urn:cdc:iisb:2014}EchoBack":
			verifyAndSet(eb, eb::getEchoBack, eb::setEchoBack);
			return true;

		case "{urn:cdc:iisb:2011}submitSingleMessage", "{urn:cdc:iisb:2014}SubmitSingleMessageRequest":
			hl7 = updateSoapMessageType(req, SubmitSingleMessageRequest.class, reader.getNamespaceURI());
			req = (SoapMessage) hl7;
			fac = (HasFacilityID) req;
			cred = (HasCredentials) req;
			verifyAttributes();
			return false;

		case "{urn:cdc:iisb:2011}submitSingleMessageResponse", "{urn:cdc:iisb:2014}SubmitSingleMessageResponse":
			hl7 = updateSoapMessageType(req, SubmitSingleMessageResponse.class, reader.getNamespaceURI());
			req = (SoapMessage) hl7;
			verifyAttributes();
			return false;

		case "{urn:cdc:iisb:2014}FacilityID", "{urn:cdc:iisb:2011}facilityID":
			verifyAndSet(fac, fac::getFacilityID, fac::setFacilityID);
			return true;

		case "{urn:cdc:iisb:2014}Hl7Message", "{urn:cdc:iisb:2011}hl7Message":
			if (hasCData(verifyAndSet(hl7, hl7::getHl7Message, hl7::setHl7Message, filter))) {
				hl7.setCdataWrapped(true);
			}
			return true;

		case "{urn:cdc:iisb:2014}Username", "{urn:cdc:iisb:2011}username":
			verifyAndSet(cred, cred::getUsername, cred::setUsername, filter);
			return true;

		case "{urn:cdc:iisb:2014}Password", "{urn:cdc:iisb:2011}password":
			verifyAndSet(cred, cred::getPassword, cred::setPassword, filter);
			return true;

		case "{http://www.w3.org/2003/05/soap-envelope}Fault":
			// This is a fault, we need to convert it to appropriate FaultResponse
			req = fm = updateSoapMessageType(req, FaultMessage.class, reader.getNamespaceURI());
			verifyAttributes();
			return false;
			
		case "{http://www.w3.org/2003/05/soap-envelope}Text":
			if (stack.peek().toString().equals("{http://www.w3.org/2003/05/soap-envelope}Reason")) {
				verifyAndSet(fm, fm::getReason, fm::setReason);
			}
			return true;
			
		case "{urn:cdc:iisb:2014}SecurityFault", 
			 "{urn:cdc:iisb:2011}SecurityFault",
			 "{urn:cdc:iisb:2014}MessageTooLargeFault", 
			 "{urn:cdc:iisb:2011}MessageTooLargeFault",
			 "{urn:cdc:iisb:hub:2014}UnsupportedOperationFault",
			 "{urn:cdc:iisb:hub:2011}UnsupportedOperationFault",
			 
			 "{urn:cdc:iisb:hub:2014}DestinationConnectionFault", 
			 "{urn:cdc:iisb:hub:2014}HubClientFault",
			 "{urn:cdc:iisb:hub:2014}MetadataFault",
			 "{urn:cdc:iisb:hub:2014}UnexpectedExceptionFault", 
			 "{urn:cdc:iisb:hub:2014}UnknownDestinationFault":
			if (fm != null) {
				verifyAndSet(null, fm::setFaultName, reader.getLocalName());
			}
			return false;
			
		case "{urn:cdc:iisb:2014}Code", "{urn:cdc:iisb:2011}code":
			verifyAndSet(fm, fm::getCode, fm::setCode);
			return true;
			
		case "{urn:cdc:iisb:2014}Detail", "{urn:cdc:iisb:2011}detail":
			verifyAndSet(fm, fm::getDetail, fm::setDetail);
			if (StringUtils.isEmpty(fm.getReason())) {
				fm.setReason(fm.getDetail());
			}
			return true;
			
		case "EventID":
			verifyAndSet(fm, fm::getEventId, fm::setEventId);
			return true;
			
		case "Summary":
			verifyAndSet(fm, fm::getSummary, fm::setSummary);
			return true;
			
		case "Detail":
			verifyAndSet(fm, null, fm::setDetail);
			if (StringUtils.isEmpty(fm.getReason())) {
				fm.setReason(fm.getDetail());
			}
			return true;
			 
		case "Diagnostics":
			verifyAndSet(fm, fm::getDiagnostics, fm::setDiagnostics);
			return true;
			
		case "Retry":
			verifyAndSet(fm, fm::getRetry, fm::setRetry);
			return true;
			
		case "{urn:cdc:iisb:2014}Size":
			verifyAndSet(fm, fm::getSize, fm::setSize);
			return true;
			
		case "{urn:cdc:iisb:2011}MaxSize":
			verifyAndSet(fm, fm::getMaxSize, fm::setMaxSize);
			return true;
			
		case "{http://www.w3.org/2003/05/soap-envelope}Code", 
			 "{http://www.w3.org/2003/05/soap-envelope}Value", 
	     	 "{http://www.w3.org/2003/05/soap-envelope}Reason", 
	     	 "{http://www.w3.org/2003/05/soap-envelope}Detail",
	     	 "{urn:cdc:iisb:2014}Reason", 
	     	 "{urn:cdc:iisb:2011}reason":
 			verifyAttributes();
			return false;
		default:
			verifyElementName();
			return false;
		}
	}

	private static boolean hasCData(int flags) {
		return (flags & (1 << XMLStreamConstants.CDATA)) != 0;
	}

	private <T extends SoapMessage> T updateSoapMessageType(SoapMessage req, Class<T> clazz, String schema) {
		T newReq = null;
		if (req.getClass() == clazz) {
			newReq = clazz.cast(req);
		} else if (req.getClass() == SoapMessage.class) {
			try {
				newReq = clazz.getConstructor(SoapMessage.class, String.class, boolean.class).newInstance(req,
						req.getSchema(), true);
			} catch (Exception e) {
				throw new IllegalArgumentException(
						"Class " + clazz.getName() + " does not implement an appropriate constructor.");
			}
		} else {
			throw new BadRequestException("Value not allowed");
		}

		newReq.setSchema(schema);
		return newReq;
	}

	private String validateText(String elementText) throws SecurityFault {
		if (isHtmlMessage) {
			return elementText;
		}
		if (!StringUtils.isEmpty(elementText) && StringUtils.containsIgnoreCase(elementText, TEXT_VALUE_PATTERN)) {
			throw SecurityFault.sourceAttack(
					"Illegal text value in " + type + " inside: <" + lastElement + "> element", endpoint);
		}
		return elementText;
	}

	private void verifyElementName() throws SecurityFault {
		String localName = reader.getLocalName();
		// Check for an HTML response
		if (documentElementName == null) {
			documentElementName = localName;
			if (localName.equalsIgnoreCase("html")) {
				isHtmlMessage = true;
			}
		}
		// If this is an HTML response, don't check for tags or attributes
		if (isHtmlMessage) {
			return;
		}
		// Check element name for illegal values.
		if (StringUtils.containsIgnoreCase(localName, TAG_NAME_PATTERN)) {
			throw SecurityFault.sourceAttack(
					"Illegal element name <" + localName + "> found in the " + type + " at: <" + lastElement,
					endpoint);
		}
		verifyAttributes();
	}

	private void verifyAttributes() throws SecurityFault {
		if (isHtmlMessage) {
			return;
		}
		// Check attribute values
		int count = reader.getAttributeCount();
		while (count-- > 0) {
			String text = reader.getAttributeValue(count);
			if (StringUtils.containsIgnoreCase(text, TEXT_VALUE_PATTERN)) {
				throw SecurityFault.sourceAttack("Illegal attribute value in " + type + " at: <" + lastElement
						+ " " + toNameString(reader.getAttributeName(count)) + "=", endpoint);
			}
		}
	}

	/**
	 * Ensure element value is not already set (indicating duplicated element).
	 * 
	 * @param content The content to check.
	 */
	private void verifyEmpty(String content) {
		if (StringUtils.isEmpty(content)) {
			return;
		}
		throw new BadRequestException(
				"Element " + reader.getPrefix() + ":" + reader.getLocalName() + " can only appear once");
	}

	private int verifyAndSet(Object holder, Supplier<String> value, Consumer<String> setter)
			throws XMLStreamException, SecurityFault {
		verifyAttributes();
		return verifyAndSet(holder, value, setter, false);
	}
	
	private int verifyAndSet(Object holder, Supplier<String> value, Consumer<String> setter, boolean filter)
			throws XMLStreamException, SecurityFault {
		if (holder != null) {
			Pair<String, Integer> v = getElementText(filter);
			verifyAndSet(value, setter, v.getLeft());
			return v.getRight();
		} else {
			throw new BadRequestException("Misplaced " + reader.getLocalName() + " element");
		}
	}
	
	private String verifyAndSet(Supplier<String> value, Consumer<String> setter, String t) throws SecurityFault {
		if (value != null) {
			verifyEmpty(value.get());
		}
		String text = validateText(t);
		setter.accept(text);
		return text;
	}
	
	private Pair<String, Integer> getElementText(boolean filter) throws XMLStreamException {
		String text = null;
		try {
			if (writer != null) {
				XmlUtils.copyStartElement(reader, writer);
			}
		} catch (XMLStreamException ignore) {
			// If the write failed, ignore it and write no more.
			writer = null;
		}
		// Don't catch on read XMLStreamException.
		Pair<String, Integer> parsedText = getElementText(); 
		
		try {
			if (writer == null) {
				return parsedText;
			}
			if (hasCData(parsedText.getRight())) {
				writer.writeCData(filter ? SoapMessageWriter.HIDDEN : text);
			} else {
				writer.writeCharacters(filter ? SoapMessageWriter.HIDDEN : text);
			}
			writer.writeEndElement();
		} catch (XMLStreamException ignore) {
			// If the write failed, ignore it and write no more.
			writer = null;
		}
		return parsedText;
	}

	public Pair<String, Integer> getElementText() throws XMLStreamException {

        if (reader.getEventType() != XMLStreamConstants.START_ELEMENT) {
            throw new XMLStreamException(
                    "parser must be on START_ELEMENT to read next text", reader.getLocation());
        }
        int eventType = reader.next();
        StringBuilder content = new StringBuilder();
        int flags = 0;
        while (eventType != XMLStreamConstants.END_ELEMENT) {
        	flags |= 1 << eventType;
            if (eventType == XMLStreamConstants.CHARACTERS
                    || eventType == XMLStreamConstants.CDATA
                    || eventType == XMLStreamConstants.SPACE
                    || eventType == XMLStreamConstants.ENTITY_REFERENCE) {
                content.append(reader.getText());
            } else if (eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
                    || eventType == XMLStreamConstants.COMMENT) {
                // skipping
            } else if (eventType == XMLStreamConstants.END_DOCUMENT) {
                throw new XMLStreamException(
                        "unexpected end of document when reading element text content");
            } else if (eventType == XMLStreamConstants.START_ELEMENT) {
                throw new XMLStreamException("elementGetText() function expects text "
                        + "only elment but START_ELEMENT was encountered.", reader.getLocation());
            } else {
                throw new XMLStreamException(
                        "Unexpected event type " + eventType, reader.getLocation());
            }
            eventType = reader.next();
        }
        return Pair.of(content.toString(), flags);
    }
	
	private static String toNameString(QName name) {
		if (name == null) {
			return null;
		}
		String prefix = name.getPrefix();
		if (StringUtils.isEmpty(prefix)) {
			return name.getLocalPart();
		}
		return name.getPrefix() + ":" + name.getLocalPart();
	}
}
