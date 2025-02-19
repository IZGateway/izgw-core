package gov.cdc.izgateway.soap;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import gov.cdc.izgateway.soap.fault.Fault;
import gov.cdc.izgateway.soap.fault.MessageTooLargeFault;
import gov.cdc.izgateway.soap.fault.MessageTooLargeFault.Direction;
import gov.cdc.izgateway.soap.fault.UnsupportedOperationFault;
import gov.cdc.izgateway.soap.fault.SecurityFault;
import gov.cdc.izgateway.soap.fault.UnexpectedExceptionFault;
import gov.cdc.izgateway.soap.fault.UnknownDestinationFault;
import gov.cdc.izgateway.soap.message.SoapMessage;
import gov.cdc.izgateway.soap.message.SubmitSingleMessageResponse;
import java.lang.reflect.InvocationTargetException;

/**
 * MockMessage provides the a Mock for an IIS that enables test driven control
 * of IIS Behavior. These messages are used when MSH-5 is "TEST", and MSH-6
 * matches one of the defined test cases in the test plan.
 */
public enum MockMessage {
	TC_01(MockMessageText.TC_01_TEXT),

	TC_02(MockMessageText.TC_02_TEXT),

	TC_03(MockMessageText.TC_03_TEXT),

	TC_04(MockMessageText.TC_04_TEXT),
	// See
	// https://repository.immregistries.org/files/resources/5835adc2add61/guidance_for_hl7_acknowledgement_messages_to_support_interoperability_.pdf#page=6
	TC_ACK02(MockMessageText.TC_ACK02_TEXT),

	TC_ACK03(MockMessageText.TC_ACK03_TEXT),

	TC_ACK04(MockMessageText.TC_ACK04_TEXT),

	TC_ACK05(MockMessageText.TC_ACK05_TEXT),

	TC_ACK06(MockMessageText.TC_ACK06_TEXT),

	TC_ACK07(MockMessageText.TC_ACK07_TEXT),

	TC_05(MockMessageText.TC_05_TEXT),

	TC_06(MockMessageText.TC_06_TEXT),

	TC_07(MockMessageText.TC_07_TEXT),

	TC_11(MockMessageText.TC_11_TEXT),

	TC_12(MockMessageText.TC_12_TEXT),

	TC_12C(TC_01, 65536),

	TC_13(MockMessageText.TC_13_TEXT),

	TC_13C(MockMessage::forceTimeout, MockMessageText.TC_13C_TEXT),

	TC_13E(MockMessage::forceTimeout, MockMessageText.TC_13E_TEXT),

	TC_14(MockMessageText.TC_14_TEXT),

	TC_17(MockMessageText.TC_17_TEXT),

	TC_18(MediaType.TEXT_HTML, MockMessageText.TC_18_TEXT), TC_18A(
			MediaType.APPLICATION_XML, MockMessageText.TC_18A_TEXT), TC_18B(
					new MediaType("application", "soap+xml"),
					MockMessageText.TC_18B_TEXT), TC_19A(
							MediaType.APPLICATION_XML,
							MockMessageText.TC_19A_TEXT), TC_19B(
									MediaType.APPLICATION_XML,
									MockMessageText.TC_19B_TEXT),

	TC_19C(MediaType.APPLICATION_XML, MockMessageText.TC_19C_TEXT),

	TC_19D(MediaType.APPLICATION_XML, MockMessageText.TC_19D_TEXT),

	TC_19E(MediaType.APPLICATION_XML, MockMessageText.TC_19E_TEXT),

	TC_19F(MediaType.APPLICATION_XML, MockMessageText.TC_19F_TEXT), TC_20(
			MockMessageText.TC_20_TEXT), TC_21A(HttpStatus.BAD_REQUEST),

	TC_21B(HttpStatus.UNAUTHORIZED),

	TC_21C(HttpStatus.FORBIDDEN),

	TC_21D(HttpStatus.NOT_FOUND),

	TC_21E(HttpStatus.METHOD_NOT_ALLOWED), TC_21F(HttpStatus.NOT_ACCEPTABLE),

	TC_21G(HttpStatus.REQUEST_TIMEOUT),

	TC_21H(HttpStatus.METHOD_NOT_ALLOWED),

	TC_21I(HttpStatus.PAYLOAD_TOO_LARGE),

	TC_21J(HttpStatus.UNSUPPORTED_MEDIA_TYPE),

	TC_21K(HttpStatus.I_AM_A_TEAPOT),

	TC_21L(HttpStatus.INTERNAL_SERVER_ERROR),

	TC_21M(HttpStatus.NOT_IMPLEMENTED),

	TC_21N(HttpStatus.BAD_GATEWAY),

	TC_21O(HttpStatus.SERVICE_UNAVAILABLE),

	TC_21P(HttpStatus.GATEWAY_TIMEOUT),

	TC_21Q(HttpStatus.LOOP_DETECTED),

	TC_21Z(MockMessage::retryableFailure, MockMessageText.TC_21Z_TEXT),

	TC_22(UnexpectedExceptionFault.class, MockMessageText.TC_22_TEXT),
	
	TC_22B(MediaType.APPLICATION_XML, MockMessageText.TC_22B_TEXT),

	TC_22D(MediaType.APPLICATION_XML, MockMessageText.TC_22D_TEXT),

	TC_22F(MediaType.APPLICATION_XML, MockMessageText.TC_22F_TEXT),
	
	TC_22S(MediaType.TEXT_HTML, MockMessageText.TC_22S_TEXT),
	
	TC_INVALID_CREDENTIALS(SecurityFault.class, MockMessageText.TC_INVALID_CREDENTIALS),
	// 2011 Fault Testing
	TC_23A(SecurityFault.class, MockMessageText.TC_23A_TEXT), TC_23C(
			MessageTooLargeFault.class, MockMessageText.TC_23C_TEXT),
	// 2011 Malformed Fault Testing
	TC_23D(MediaType.APPLICATION_XML, MockMessageText.TC_23D_TEXT),

	TC_23E(MediaType.APPLICATION_XML, MockMessageText.TC_23E_TEXT,
			HttpStatus.BAD_REQUEST),

	TC_23F(MediaType.APPLICATION_XML, MockMessageText.TC_23F_TEXT,
			HttpStatus.BAD_REQUEST),

	TC_23G(MediaType.APPLICATION_XML, MockMessageText.TC_23G_TEXT,
			HttpStatus.BAD_REQUEST),
	// 2014 Fault Testing
	TC_24A(SecurityFault.class, MockMessageText.TC_24A_TEXT),

	TC_24C(MessageTooLargeFault.class, MockMessageText.TC_24C_TEXT),
	// 2014 Malformed Fault Testing
	TC_24D(MediaType.APPLICATION_XML, MockMessageText.TC_24D_TEXT),

	TC_24F(MediaType.APPLICATION_XML, MockMessageText.TC_24F_TEXT,
			HttpStatus.BAD_REQUEST),

	TC_24G(MediaType.APPLICATION_XML, MockMessageText.TC_24G_TEXT,
			HttpStatus.BAD_REQUEST),

	TC_24H(MediaType.APPLICATION_XML, MockMessageText.TC_24H_TEXT,
			HttpStatus.BAD_REQUEST), 

	TC_24I(MockMessageText.TC_24I_TEXT),
	// PHI Masking in faults
	TC_25(MediaType.APPLICATION_XML, MockMessageText.TC_25_TEXT, HttpStatus.INTERNAL_SERVER_ERROR),
	TC_UNKF(MockMessage::simulateFault,
					MockMessageText.TC_UNKF_TEXT);


	public static final MockMessage TC_FORCE_TIMEOUT = TC_13C;
	private static int retryableRequestCount = 0;
	/**
	 * Wrapped fault is used to identify exceptions which the Mock intends to
	 * throw
	 */
	private static class WrappedFault extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private WrappedFault(Throwable t) {
			super("Intentional Exception throw by a MockMessage, should never leave Mock",
					t);
		}
	}

	/**
	 * Mock fault is used to identify exceptions which the Mock didn't intend to
	 * throw
	 */
	public static class MockFault extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private MockFault(Throwable t) {
			super("Unexpected error in MockMessage execution", t);
		}
	}

	@FunctionalInterface
	public interface FaultingFunction<T, R> {
		R apply(T t) throws Fault;
	}
	/**
	 * The Function producing response behaviors that are to be produced for the
	 * IIS
	 */
	private final FaultingFunction<SoapMessage, ResponseEntity<?>> body;
	/** The expected result */
	private final Object expected;

	/**
	 * Constructor for simple mocks that return a fixed message. These fixed
	 * messages are to test IZ Gateway behavior that is driven by message
	 * content, mostly with regard to logging.
	 *
	 * @param message
	 *            The message to return.
	 */
	MockMessage(String message) {
		SubmitSingleMessageResponse t = new SubmitSingleMessageResponse(
				message);
		expected = t;
		body = m -> new ResponseEntity<SubmitSingleMessageResponse>(t,
				HttpStatus.OK);
	}

	MockMessage(MockMessage response, int size) {
		this(repeatString(response, size));
	}

	private static String repeatString(MockMessage response, int size) {
		String value = ((SubmitSingleMessageResponse) response.getExpected())
				.getHl7Message();
		StringBuilder newMessage = new StringBuilder();
		do {
			newMessage.append(value);
		} while (size >= newMessage.length());
		return newMessage.toString();
	}

	/**
	 * Constructor for mocks designed to act as an IIS that is failing to
	 * connect because it is either down for maintenance, or stuck behind a
	 * firewall.
	 *
	 * @param status
	 *            The HTTP Status Code to return
	 */
	MockMessage(HttpStatus status) {
		this(MediaType.APPLICATION_XML, "", status);
	}

	/**
	 * Constructor for mocks designed to act as an IIS that is returning
	 * non-conforming content in response to IZ Gateway requests.
	 *
	 * @param type
	 *            The media type of the content.
	 * @param content
	 *            The text body of the content.
	 */
	MockMessage(MediaType type, String content) {
		this(type, content, HttpStatus.OK);
	}

	/**
	 * Constructor for mocks designed to act as an IIS that is returning
	 * non-conforming content in response to IZ Gateway requests.
	 *
	 * @param type
	 *            The media type of the content.
	 * @param content
	 *            The text body of the content.
	 * @param status
	 *            The Http Status code to return.
	 */
	MockMessage(MediaType type, String content, HttpStatus status) {
		ResponseEntity<String> entity = ResponseEntity.status(status)
				.contentType(type).body(content);
		this.body = m -> entity;
		this.expected = entity;
	}

	/**
	 * Constructor for functions that have specialized behaviors (e.g., Timeout,
	 * which needs to go to sleep to simulate a non-responsive IIS.
	 *
	 * @param body
	 *            The response to return.
	 * @param expected
	 *            The expected value.
	 */
	MockMessage(FaultingFunction<SoapMessage, ResponseEntity<?>> body,
			Object expected) {
		this.body = body;
		this.expected = expected;
	}

	/**
	 * Constructor for mocks that return SOAP Faults of various types.
	 *
	 * @param t
	 *            The SOAP Fault that should appear.
	 * @param message
	 *            The message explaining the reason for failure.
	 * @param mutant
	 *            Mutated form to use (make invalid according to schema)
	 */
	MockMessage(Class<? extends Throwable> t, String message) {
		Throwable throwable;
		try {
			if (SecurityFault.class.isAssignableFrom(t)) {
				throwable = SecurityFault.generalSecurity(message, null,
						new Exception("Mock Security Fault"));
			} else if (UnexpectedExceptionFault.class.isAssignableFrom(t)) {
				throwable = new UnexpectedExceptionFault(message,
						new Exception("Mock Unexpected Exception Fault"),
						"Fault generated by " + this.name());
			} else if (MessageTooLargeFault.class.isAssignableFrom(t)) {
				throwable = new MessageTooLargeFault(Direction.REQUEST, 1, 0);
			} else {
				throwable = t.getConstructor(String.class).newInstance(message);
			}
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new MockFault(e);
		}
		this.body = m -> {
			throw new WrappedFault(throwable);
		};
		this.expected = throwable;
	}

	/**
	 * Get the message response this mock is intended to return.
	 *
	 * @param message
	 *            The SOAP message object, used again to return a non-XML body.
	 * @return The mocked response
	 * @throws Fault To mock a generic fault
	 * @throws SecurityFault
	 *             To mock a security fault.
	 * @throws MessageTooLargeFault
	 *             To mock a MessageTooLargeFault
	 * @throws UnsupportedOperationFault
	 *             To mock an unsupported operation
	 */
	public ResponseEntity<?> getMessage(SoapMessage message) // NOSONAR ? is
																// intentional
			throws Fault {
		try {
			return body.apply(message);
		} catch (WrappedFault ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof MessageTooLargeFault mtlf) {
				throw mtlf;
			} else if (cause instanceof SecurityFault secf) {
				throw secf;
			} else if (cause instanceof UnsupportedOperationFault uof) {
				throw uof;
			} else if (cause instanceof Fault f) {
				throw f;
			} else {
				throw ex;
			}
		}
	}

	/**
	 * Return the object that describes the expected behavior of this mock. This
	 * is used for unit testing of the Mock.
	 *
	 * This will be: A SubmitSingleMessageResponse for normal transmission and
	 * responses A ResponseEntity for non-SOAP Bodies An Exception for
	 * exceptions that are intentionally thrown.
	 *
	 * @return the object that describes the expected behavior
	 */
	public Object getExpected() {
		return expected;
	}

	/**
	 * Get a MockMessage cooresponding to an identifier. Used by IISService to
	 * determine which messages should be responded to by a mock.
	 *
	 * @param identifier
	 *            The test case identifier (the enumeration name) to find.
	 * @return The MockMessage that will execute that test case.
	 */
	public static MockMessage getMock(String identifier) {
		try {
			return Enum.valueOf(MockMessage.class, identifier);
		} catch (IllegalArgumentException | NullPointerException e) {
			return null;
		}
	}

	/**
	 * The function call that the timeout mock uses to sleep beyond IZ Gateway's
	 * patience.
	 * 
	 * @param resp
	 *            The HttpServletResponse, not used.
	 * @param msg
	 *            The Message, also not used.
	 * @return An empty response.
	 */
	private static ResponseEntity<?> forceTimeout(SoapMessage msg) {
		try {
			Thread.sleep(62000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		SubmitSingleMessageResponse sr = new SubmitSingleMessageResponse("");
		sr.setSchema(msg.getSchema());
		return new ResponseEntity<>(sr, HttpStatus.OK);
	}

	private static ResponseEntity<?> simulateFault(SoapMessage msg)
			throws Fault {
		throw UnknownDestinationFault
				.missingDestination("There is no destination in the request");
	}
	/**
	 * This method fails on odd requests (first, third, et cetera) And succeeds
	 * on even requests. If running with a solo client that has sticky sessions,
	 * this enables the client to verify retry attempts.
	 * 
	 * @param resp
	 *            The response
	 * @param msg
	 *            The message
	 * @return A response that fails on odd attempts and succeeds on even ones.
	 * @throws Fault
	 */
	private static ResponseEntity<?> retryableFailure(SoapMessage msg)
			throws Fault {
		// Succeed on even requests (second, fourth, et cetera).
		if (incrementRequestCount()) {
			return TC_04.body.apply(msg);
		}
		// Fail on odd requests (first, third, et cetera)
		return TC_21D.body.apply(msg);
	}

	private static boolean incrementRequestCount() {
		return ++retryableRequestCount % 2 == 0;
	}
}

class MockMessageText {
	private MockMessageText() {
	}
	static final String ENVELOPE = "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'>";
	static final String TC_02_TEXT = "MSH|^~\\&|DSTAPP^^|DSTFAC^^|SRCAPP^^|SRCFAC^^|20211215140314-0500||ACK^V04^ACK|9744810662.100002804|P|2.5.1|||NE|NE|||||Z23^CDCPHINVS|\r"
			+ "MSA|AE|0b4c2cbc-d3e6-496f-935e-79d4e503e4ce|\r";
	static final String TC_01_TEXT = TC_02_TEXT
			+ "ERR||PD1^^16|101^Required field missing^HL70357|W||||patient immunization registry status is missing|\r";
	static final String MESSAGE_4 = "xmlns:urn='urn:cdc:iisb:hub:2014' xmlns:urn1='urn:cdc:iisb:2014'><soap:Body/></soap:Envelope>";
	static final String MESSAGE_3 = "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope' "
			+ MESSAGE_4;
	static final String TC_03_TEXT = TC_02_TEXT
			+ "ERR||PID^1^24|101^Required field missing^HL70357|W||||patient multiple birth indicator is missing|\r";

	static final String TC_04_TEXT = "MSH|^~\\&|IRIS IIS|IRIS||IZG|20220205||RSP^K11^RSP_K11|20210330093013AZQ231|P|2.5.1|||||||||Z32^CDCPHINVS\r"
			+ "MSA|AA|20210330093013AZQ231||0||0^Message Accepted^HL70357\r"
			+ "QAK|20210330093013AZQ231|NF|Z34^Request Complete Immunization history^CDCPHINVS\r"
			+ "QPD|Z34^Request Immunization History^CDCPHINVS|20210330093013IA231|112258-9^^^IA^MR|"
			+ "JohnsonIZG^JamesIZG^AndrewIZG^^^^L|LeungIZG^SarahIZG^^^^^M|20160414|M|"
			+ "Main Street&&123^^Adel^IA^50003^^L|^PRN^PH^^^555^5551111|Y|1\r";

	// See
	// https://repository.immregistries.org/files/resources/5835adc2add61/guidance_for_hl7_acknowledgement_messages_to_support_interoperability_.pdf#page=6
	static final String TC_ACK02_TEXT = "MSH|^~\\&|DCS|MYIIS|MYIIS||20150924161633-0500||ACK^V04^ACK|5315315|P|2.5.1|||NE|NE|||||Z23^CDCPHINVS\r"
			+ "MSA|AA|4513185\r"
			+ "ERR|||0^Message Accepted^HL70357|I||||3 of 3 immunizations have been added to IIS";

	static final String TC_ACK03_TEXT = "MSH|^~\\&|DCS|MYIIS|MYIIS||20150924162038-0500||ACK^V04^ACK|465798|P|2.5.1|||NE|NE|||||Z23^CDCPHINVS\r"
			+ "MSA|AE|313217\r"
			+ "ERR||PID^1^11^5|999^Application error^HL70357|W|1^illogical date error^HL70533|||12345 is not a valid zip code in MYIIS";

	static final String TC_ACK04_TEXT = "MSH|^~\\&|DCS|MYIIS|MYIIS||20150924162338-0500||ACK^V04^ACK|6157|P|2.5.1|||NE|NE|||||Z23^CDCPHINVS\r"
			+ "MSA|AE|783843\r"
			+ "ERR||PID^1^11^5|999^Application error^HL70357|W|1^illogical date error^HL70533|||12345 is not a valid zip code in MYIIS\r"
			+ "ERR||PID^1^7|101^required field missing^HL70357|E||||Birth Date is required.\r";
	static final String TC_ACK05_TEXT = "MSH|^~\\&|DCS|MYIIS|MYIIS||20150924162038- 0500||ACK^V04^ACK|6516848|P|2.5.1|||NE|NE|||||Z23^CDCPHINVS\r"
			+ "MSA|AE|165138\r"
			+ "ERR|||0^Message Accepted^HL70357|I||||3 of 3 immunizations have been added to IIS\r"
			+ "ERR||PID^1^11^5|999^Application error^HL70357|W|1^illogical date error^HL70533|||12345 is not a valid zip code in MYIIS\r";

	static final String TC_ACK06_TEXT = "MSH|^~\\&|DCS|MYIIS|MYIIS||20150924162038- 0500||ACK^V04^ACK|987648|P|2.5.1|||NE|NE|||||Z23^CDCPHINVS\r"
			+ "MSA|AE|1531573\r"
			+ "ERR||PID^1^7|101^required field missing^HL70357|E||||Birth Date is required.\r";
	static final String TC_ACK07_TEXT = "MSH|^~\\&|DCS|MYIIS|MYIIS||20150924162338-0500||ACK^V04^ACK|13549|P|2.5.1|||NE|NE|||||Z23^CDCPHINVS\r"
			+ "MSA|AR|9299381\r"
			+ "ERR||MSH^1^12|203^unsupported version id^HL70357|E||||Unsupported HL7 Version ID\r";
	static final String TC_05_TEXT = TC_02_TEXT
			+ "ERR||ORC^^3|101^Required field missing^HL70357|W||||vaccination id is missing|\r";
	static final String TC_06_TEXT = TC_02_TEXT
			+ "ERR||RXA^1^5^^1|103^Table value not found^HL70357|W||||vaccination cvx code is unrecognized|\r";
	static final String TC_07_TEXT = "MSH|^~\\&|HOSPMPI^^|HOSP^^|CLINREG^^|WESTCLIN^^|20211215143242-0500||ACK^Q21^ACK|7753562890.100002817|D|2.5.1|||NE|NE|||||Z23^CDCPHINVS|\r"
			+ "MSA|AR|1|\r"
			+ "ERR||MSH^1^11|202^Unsupported Processing ID^HL70357|E||||The only Processing ID used to submit HL7 messages to the IIS is \"P\". Debug mode cannot be used. - Message Rejected|\r";
	static final String TC_11_TEXT = "DNS Lookup Fault";
	static final String TC_12_TEXT = "TCP SYN Communication Fault";
	static final String TC_13_TEXT = "SYN Request Rejection Fault";
	static final String TC_13C_TEXT = "This should have timed out";
	static final String TC_13E_TEXT = "This should have timed out"; // Duplicated
																	// to match
																	// actual
																	// test case
																	// ID.
	static final String TC_14_TEXT = "Destination IIS has an valid certificate but is untrusted";
	static final String TC_15_TEXT = "Destination IIS has an invalid certificate";
	static final String TC_16_TEXT = "Destination IIS has an certificate has an invalid cypher";
	static final String TC_17_TEXT = "Destination IIS rejected IZ Gateway certificate";
	static final String TC_18_TEXT = "<html><body><h1>TC_IIS_8A</h1></body></html>";
	static final String TC_18A_TEXT = "<html><body><h1>TC_IIS_8A</h1></body></html>";
	static final String TC_18B_TEXT = MockMessageText.MESSAGE_3;

	static final String TC_19A_TEXT = "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope' "
			+ "xmlns:urn='urn:cdc:iisb:hub:2014' xmlns:urn1='urn:cdc:iisb:2014'><soap:Body/&gt;</soap:Envelope>";
	static final String TC_19B_TEXT = "\0x07" + MESSAGE_3;

	static final String TC_19C_TEXT = "?" + MESSAGE_3;

	static final String TC_19D_TEXT = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope' "
			+ MockMessageText.MESSAGE_4;

	static final String TC_19E_TEXT = "<soap:Envelope xmlns:soap-'http://www.w3.org/2003/05/soap-envelope' "
			+ MockMessageText.MESSAGE_4;

	static final String TC_19F_TEXT = "<s:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope' "
			+ MockMessageText.MESSAGE_4;

	static final String TC_20_TEXT = "Invalid 2011/2014 Message";
	static final String TC_21Z_TEXT = "Retryable Failure";
	static final String TC_22_TEXT = "SOAP Fault";

	static final String TC_INVALID_CREDENTIALS = "Invalid Username, Password or FacilityID";
	// 2011 Fault Testing
	static final String TC_23A_TEXT = "Invalid Username, Password or FacilityID";
	static final String TC_23C_TEXT = "Message Too Large Fault Testing";
	// 2011 Malformed Fault Testing
	static final String TC_23D_TEXT = ENVELOPE
			+ "<soap:Fault><soap:Code><soap:Value>soap:Receiver</soap:Value>"
			+ "</soap:Code><soap:Reason><soap:Text>Invalid Username, Password or FacilityID</soap:Text>"
			+ "</soap:Reason><soap:Detail><ns3:SecurityFault xmlns:ns3='urn:cdc:iisb:2011'>"
			// Change from required "Security" to "Security Fault"
			+ "<ns3:Code>401</ns3:Code><ns3:Reason>Security Fault"
			+ "</ns3:Reason><ns3:Detail>Invalid Username, Password or FacilityID"
			+ "</ns3:Detail></ns3:SecurityFault></soap:Detail></soap:Fault></soap:Envelope>";
	static final String TC_23E_TEXT = ENVELOPE
			+ "<soap:Fault><soap:Code><soap:Value>soap:Receiver</soap:Value></soap:Code><soap:Reason>"
			+ "<soap:Text>Invalid Username, Password or FacilityID</soap:Text></soap:Reason>"
			+ "<soap:Detail><ns3:SecurityFault xmlns:ns3='urn:cdc:iisb:2011'>"
			// removed ns3: prefix
			+ "<Code>401</Code><Reason>Security</Reason><Detail>Invalid Username, Password or FacilityID</Detail>"
			+ "</ns3:SecurityFault></soap:Detail></soap:Fault></soap:Envelope>";

	static final String TC_23F_TEXT = "<Envelope xmlns='http://www.w3.org/2003/05/soap-envelope'><Fault>"
			// Removed all ns prefixes
			// Without ns prefixes, these are assumed to be SOAP, which is
			// invalid
			+ "<Code><Value>Receiver</Value></Code>"
			+ "<Reason><Text>Invalid Username, Password or FacilityID</Text></Reason>"
			+ "<Detail>" + "<SecurityFault>" + "<Code>401</Code>"
			+ "<Reason>Security</Reason>"
			+ "<Detail>Invalid Username, Password or FacilityID</Detail>"
			+ "</SecurityFault>" + "</Detail></Fault></Envelope>";

	static final String TC_23G_TEXT = ENVELOPE
			+ "<soap:Fault><soap:Code><soap:Value>soap:Receiver</soap:Value></soap:Code>"
			+ "<soap:Reason><soap:Text>Invalid Username, Password or FacilityID</soap:Text></soap:Reason>"
			+ "<soap:Detail><ns3:SecurityFault xmlns:ns3='urn:cdc:iisb:2011'>"
			+ "<ns3:Code>401</ns3:Code>" + "<ns3:Reason>Security</ns3:Reason>"
			+ "<ns3:Detail>Invalid Username, Password or FacilityID</ns3:Detail>"
			+ "</ns3:SecurityFault></soap:Detail></soap:Fault></soap:Envelope>";
	// 2014 Fault Testing
	static final String TC_24A_TEXT = "Invalid Username, Password or FacilityID";

	static final String TC_24C_TEXT = "Message Too Large Fault Testing";
	// 2014 Malformed Fault Testing
	static final String TC_24D_TEXT = ENVELOPE
			+ "<soap:Fault><soap:Code><soap:Value>soap:Receiver</soap:Value></soap:Code><soap:Reason>"
			+ "<soap:Text xml:lang='en'>Request Message Too Large: 0 exceeds 0</soap:Text>"
			+ "</soap:Reason><soap:Detail>"
			+ "<ns2:MessageTooLargeFault xmlns:ns3='urn:cdc:iisb:2011' xmlns:ns2='urn:cdc:iisb:2014' xmlns='urn:cdc:iisb:hub:2014'>"
			+ "<ns2:Size>A</ns2:Size>" // Should contain integer value
			+ "<ns2:MaxSize>B</ns2:MaxSize>" // Should contain integer
												// value
			+ "</ns2:MessageTooLargeFault>"
			+ "<Summary>RequestMessageTooLarge</Summary>"
			+ "<Retry>CORRECT_MESSAGE</Retry></soap:Detail></soap:Fault></soap:Envelope>";

	static final String TC_24F_TEXT = ENVELOPE
			// Should be inside <soap:Value> element
			+ "<soap:Fault><soap:Code>soap:Receiver</soap:Code>"
			+ "<soap:Reason>Request Message Too Large: 0 exceeds 0</soap:Reason>"
			// Should be inside <soap:Text> element
			+ "<soap:Detail><MessageTooLargeFault/>" // No namespace here
			+ "<Summary>RequestMessageTooLarge</Summary>"
			+ "<Retry>CORRECT_MESSAGE</Retry></soap:Detail></soap:Fault></soap:Envelope>";

	static final String TC_24G_TEXT = ENVELOPE
			+ "<soap:Fault><soap:Code><soap:Value>soap:Receiver</soap:Value></soap:Code>"
			+ "<soap:Reason><soap:Text>Invalid Username, Password or FacilityID</soap:Text></soap:Reason><soap:Detail>"
			+ "<ns3:SecurityFault xmlns:ns3='urn:cdc:iisb:2014'>"
			+ "<ns3:Code>401</ns3:Code><ns3:Reason>Security</ns3:Reason>" 
			+ "<ns3:Detail>Invalid Username, Password or FacilityID</ns3:Detail>"
			+ "</ns3:SecurityFault>"
			+ "</soap:Detail></soap:Fault></soap:Envelope>";

	static final String TC_24H_TEXT = "This is not a SOAP Fault nor is it XML";

	static final String TC_24I_TEXT = "MSH|^~\\&|IRIS IIS|IRIS||IZG|20220205||RSP^K11^RSP_K11|20210330093013AZQ231|P|2.5.1|||||||||Z32^CDCPHINVS\r"
			+ "MSA|AA|20210330093013AZQ231||0||0^Message Accepted^HL70357" + Character.valueOf((char)0x13)
			+ "QAK|20210330093013AZQ231|NF|Z34^Request Complete Immunization history^CDCPHINVS\r"
			+ "QPD|Z34^Request Immunization History^CDCPHINVS|20210330093013IA231|112258-9^^^IA^MR|"
			+ "JohnsonIZG^JamesIZG^AndrewIZG^^^^L|LeungIZG^SarahIZG^^^^^M|20160414|M|"
			+ "Main Street&&123^^Adel^IA^50003^^L|^PRN^PH^^^555^5551111|Y|1\r";
	
	
	static final String TC_UNKF_TEXT = "Unknown Exception";

	static final String TC_22B_TEXT = ENVELOPE + "" + "<soap:Fault>"
			+ "<soap:Code><soap:Value>soap:Receiver</soap:Value></soap:Code><soap:Reason>"
			+ "<soap:Text xml:lang='en'>Request Message contains forbidden patterns</soap:Text></soap:Reason><soap:Detail>"
			+ "<ns2:SecurityFault xmlns:ns3='urn:cdc:iisb:2011' xmlns:ns2='urn:cdc:iisb:2014' xmlns='urn:cdc:iisb:hub:2014'></ns2:SecurityFault>"
			+ "<Summary>Source Attack Pattern Detected</Summary>"
			+ "<Retry>CORRECT_MESSAGE</Retry>"
			+ "<Script>Forbidden message patterns causing an error</Script>"
			+ "</soap:Detail></soap:Fault></soap:Envelope>";
	static final String TC_22D_TEXT = ENVELOPE + "" + "<soap:Fault>"
			+ "<soap:Code>" + "<soap:Value>soap:Receiver</soap:Value>"
			+ "</soap:Code>" + "<soap:Reason>"
			+ "<soap:Text xml:lang='en'>Request Message contains forbidden patterns</soap:Text>"
			+ "</soap:Reason>" + "<soap:Detail>"
			+ "<ns2:SecurityFault xmlns:ns3='urn:cdc:iisb:2011' xmlns:ns2='urn:cdc:iisb:2014' xmlns='urn:cdc:iisb:hub:2014'>"
			+ "</ns2:SecurityFault>"
			+ "<Summary>Source Attack Pattern Detected</Summary>"
			+ "<Retry script=\"javascript:alert('This is an attribute in error')\">CORRECT_MESSAGE</Retry>"
			+ "</soap:Detail>" + "</soap:Fault></soap:Envelope>";
	static final String TC_22F_TEXT = ENVELOPE
			+ "<soap:Fault><soap:Code><soap:Value>soap:Receiver</soap:Value></soap:Code><soap:Reason>"
			+ "<soap:Text xml:lang='en'>Request Message contains forbidden patterns</soap:Text></soap:Reason><soap:Detail>"
			+ "<ns2:SecurityFault xmlns:ns3='urn:cdc:iisb:2011' xmlns:ns2='urn:cdc:iisb:2014' xmlns='urn:cdc:iisb:hub:2014'>"
			+ "</ns2:SecurityFault>"
			+ "<Summary>javascript:alert('This is text in error')</Summary>"
			+ "<Retry>CORRECT_MESSAGE</Retry>" + "</soap:Detail>"
			+ "</soap:Fault></soap:Envelope>";
	static final String TC_25_TEXT = "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'>"
			+ "<soap:Body>\r\n"
			+ "<soap:Fault>\r\n"
			+ "<soap:Code><soap:Value>soap:Sender</soap:Value></soap:Code>\r\n"
			+ "<soap:Reason><soap:Text xml:lan=\"en-US\">\r\n"
			+ "Authentication Error Occurred. Inspect the HL7 Response message for more details.\r\n"
			+ "\r\n"
			+ "Original HL7 Response\r\n"
			+ "MSH|^\\~&amp;|WebIZ.24.12.0.21|KS0000|TestApplication|KSHL71234|20231115123051.679-0700||RSP^K11^RSP_K11|KS000020231115123051167|D|2.5.1|||NE|NE|||||Z33^CDCPHIVS\r"
			+ "MSA|AE|KS999938854000000232\r\n"
			+ "ERR||MSH^1^6^1^1~MSH^1^4^1^1|999^Application Error^HL70357|E|4^Invalid value^HL70533^WEBIZ-AUTH-609^Invalid Receiving Facility for Incoming Message\r"
			+ "QPD|Z24^Request Immunization Hiistory|CDCPHINVS|querytag||SIMPSON^BART^^^^L19990101\r\n"
			+ "</soap:Text></soap:Reason>"
			+ "<soap:Detail>"
			+ "<soap:Text xml:lan=\"en-US\">"
			+ "Authentication Error Occurred. Inspect the HL7 Response message for more details.\n"
			+ "\n"
			+ "Original HL7 Response\n"
			+ "MSH|^\\~&amp;|WebIZ.24.12.0.21|KS0000|TestApplication|KSHL71234|20231115123051.679-0700||RSP^K11^RSP_K11|KS000020231115123051167|D|2.5.1|||NE|NE|||||Z33^CDCPHIVS\r\n"
			+ "MSA|AE|KS999938854000000232\n"
			+ "ERR||MSH^1^6^1^1~MSH^1^4^1^1|999^Application Error^HL70357|E|4^Invalid value^HL70533^WEBIZ-AUTH-609^Invalid Receiving Facility for Incoming Message\r"
			+ "QPD|Z24^Request Immunization Hiistory|CDCPHINVS|querytag||SIMPSON^BART^^^^L19990101\r\n"
			+ "More text"
			+ "</soap:Text>\r\n"
			+ "</soap:Detail>"
			+ "</soap:Fault></soap:Body></soap:Envelope>";
	static final String TC_22S_TEXT = "<!DOCTYPE html>\r\n"
			+ "<!--/secure/error.jsp-->\r\n"
			+ "<!--/WEB-INF/jspf/standardHeader.jsp-->\r\n"
			+ "<html lang=\"en-US\">\r\n" + "  <head>\r\n"
			+ "    <title>PHC Hub Registry Importer Web Application Error Page</title>\r\n"
			+ "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\r\n"
			+ "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\r\n"
			+ "    <link rel=\"stylesheet\" href=\"/testphc/cache/00caf2989581d8ee7fa9fec6ac6ac440/struts/css_xhtml/styles.css\">\r\n"
			+ "        <link rel=\"stylesheet\" href=\"/testphc/cache/840cca94d5e33faf324bcd4988fa8401/css/jquery-ui-css/jquery-ui.min.css\">\r\n"
			+ "        <link rel=\"stylesheet\" href=\"/testphc/cache/ad157990ab4968a16f3c2dd26518af1d/css/chosen/chosen.min.css\">\r\n"
			+ "        <link rel=\"stylesheet\" href=\"/testphc/cache/0e6ba998104b91868b2a8d2375d340e7/css/datatables/css/jquery.dataTables.min.css\">\r\n"
			+ "        <link rel=\"stylesheet\" href=\"/testphc/cache/4679a941453d3eea0e5a7d5222d338c1/css/jquery-ui-css/jquery.ui.timepicker.css\">\r\n"
			+ "        <link rel=\"stylesheet\" href=\"/testphc/cache/6a7d51a6d265b4ec2771abca088812ba/css/peraltahl7.css\">\r\n"
			+ "        <link rel=\"stylesheet\" href=\"/testphc/cache/23a35f45b69017fbfb2ec8c44538e627/css/printPreview.css\"  media=\"print\">\r\n"
			+ "      <script type=\"text/javascript\">\r\n"
			+ "        var contextPath = \"/testphc\";\r\n" + "        \r\n"
			+ "        document.contextPath = contextPath;\r\n"
			+ "      </script>\r\n"
			+ "      <script src=\"/testphc/cache/3c198bb745496d1069014c2f354a76dc/struts/utils.js\"></script>\r\n"
			+ "    \r\n"
			+ " <!--------------- walkMe snippet ends--------- -->\r\n"
			+ "  </head><body><p onclick='javascript:alert(\"message\")'>javascript:alert('message;)</p></body></html>";
}
