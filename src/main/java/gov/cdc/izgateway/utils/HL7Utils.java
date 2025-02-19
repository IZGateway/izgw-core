package gov.cdc.izgateway.utils;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.UnaryOperator;
import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

/**
 * Static methods for working with HL7 Message content. This mostly is used to
 * filter out only the metadata content of HL7 Messages to just reflect that in
 * logs without including any PHI.
 */
public class HL7Utils {
    public static final Map<String, Collection<Integer>> DEFAULT_ALLOWED_SEGMENTS = new TreeMap<>();
    public static final String ETC = "...";

    static {
    	DEFAULT_ALLOWED_SEGMENTS.put("MSH", Arrays.asList(1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 21));
    	DEFAULT_ALLOWED_SEGMENTS.put("MSA", Arrays.asList(1, 2, 3));
    	DEFAULT_ALLOWED_SEGMENTS.put("QAK", Arrays.asList(1, 2, 3));
    	DEFAULT_ALLOWED_SEGMENTS.put("ERR", Arrays.asList(1, 2, -3, 4));
    }

	private HL7Utils() {
		// Do nothing
	}

	/**
	 * Given a map of allowed segments and fields, and a message, remove all parts
	 * that aren't explicitely allowed.
	 *
	 * @param message         The message to protect.
	 * @param allowedSegments A map of allowed segments to allowed fields. An empty
	 *                        collection allows all fields.
	 * @param etcSuffix       Suffix to insert to denote removed content.
	 * @return The protected message.
	 */
	public static String protectHL7Message(String message, Map<String, Collection<Integer>> allowedSegments,
			String etcSuffix) {

		if (StringUtils.isEmpty(message)) {
			return message;
		}

		if (StringUtils.isEmpty(etcSuffix)) {
			etcSuffix = "";
		} else {
			etcSuffix += "\n";
		}

		String[] segments = message.split("\\s*[\\n\\r]+");
		StringBuilder b = new StringBuilder();
		for (String segment : segments) {
			String segName = StringUtils.substringBefore(segment, "|");
			Collection<Integer> allowedFields = allowedSegments.get(segName);
			if (!"MSH".equals(segName) && allowedFields != null && !allowedFields.isEmpty()) {
				allowedFields = adjustNonMSHAllowedValues(allowedFields);
			}
			if (allowedFields == null) {
				if (!StringUtils.endsWith(b, etcSuffix)) {
					b.append(etcSuffix);
				}
			} else if (allowedFields.isEmpty()) {
				b.append(segment).append("\n");
			} else {
				b.append(stripSegment(segment, allowedFields)).append("\n");
			}
		}
		String result = b.toString();
		return etcSuffix.equals(result) ? "" : result;
	}
	
	public static String protectHL7Message(String hl7Message) {
		return HL7Utils.protectHL7Message(hl7Message, DEFAULT_ALLOWED_SEGMENTS, ETC);
	}

	/**
	 * Adjust field numbers for nonMSH segments in allowedFields.
	 * 
	 * @param allowedFields The collection to adjust
	 * @return The adjusted collection.
	 */
	private static Collection<Integer> adjustNonMSHAllowedValues(Collection<Integer> allowedFields) {
		List<Integer> values = new ArrayList<>();
		for (int value : allowedFields) {
			values.add(value < 0 ? value - 1 : (value + 1));
		}
		values.add(1);
		allowedFields = values;
		return allowedFields;
	}

	/**
	 * Strip an HL7 Segment of allowed fields
	 * 
	 * @param segment       The segment to strip.
	 * @param allowedFields The set of allowed fields.
	 * @return The stripped segment
	 */
	public static String stripSegment(String segment, Collection<Integer> allowedFields) {
		return stripParts(segment, allowedFields, "|", HL7Utils::stripCWE);
	}

	/**
	 * Strip uncontrolled text from an HL7 CWE type
	 * 
	 * @param cwe The CWE field to strip
	 * @return The stripped CWE field
	 */
	public static String stripCWE(String cwe) {
		return stripParts(cwe, Arrays.asList(1, 3, 4, 6), "^", null);
	}

	/**
	 * Given a string in whole, a part delimiter in delim, and a list of allowed
	 * parts (by ordinal position), return the string with only the allowed parts.
	 * If an ordinal value in allowedParts is positive, the part is allowed, if
	 * negative, then is allowed after further stripping by passed in stripper
	 * function.
	 * 
	 * @param whole        The string to strip
	 * @param allowedParts A list of allowed parts to retain
	 * @param delim        The delimiter
	 * @param stripper     An additional stripping function for parts needing
	 *                     further work
	 * @returns The stripped string.
	 */
	private static String stripParts(String whole, Collection<Integer> allowedParts, String delim,
			UnaryOperator<String> stripper) {
		if (StringUtils.isEmpty(whole)) {
			return whole;
		}
		StringBuilder b = new StringBuilder();
		String[] fields = whole.split("\\" + delim);
		int lastPart = 0;
		for (int i = 0; i < fields.length; i++) {
			if (stripper != null && allowedParts.contains(-(i + 1))) {
				b.append(stripper.apply(fields[i]));
				lastPart = i;
			} else if (allowedParts.contains(i + 1)) {
				b.append(fields[i]);
				lastPart = i;
			}
			b.append(delim);
		}
		b.setLength(b.length() - (fields.length - lastPart));
		return b.toString();
	}

	public static class HL7Message {
		public static final char[] SEGMENT_SEPARATORS = { '\n', '\r' };
		public static final int SEGMENT_NAME = 0;
		public static final int FIELD_SEPARATOR = 1;
		public static final int ENCODING_CHARACTERS = 2;
		public static final int SENDING_APPLICATION = 3;
		public static final int SENDING_FACILITY = 4;
		public static final int RECEIVING_APPLICATION = 5;
		public static final int RECEIVING_FACILITY = 6;
		public static final int MESSAGE_DATETIME = 7;
		public static final int SECURITY = 8;
		public static final int MESSAGE_TYPE = 9;
		public static final int MESSAGE_CONTROL_ID = 10;
		public static final int SENDING_RESPONSIBLE_ORGANIZATION = 22;
		
		@Getter
		private final String hl7Message;
		@Getter
		private final String msh;
		private final String[] parts;
		public HL7Message(final String hl7Message) {
			this.hl7Message = hl7Message;
			if (hl7Message == null) {
				msh = null;
				parts = new String[0];
			} else {
				int pos = StringUtils.indexOfAny(hl7Message, SEGMENT_SEPARATORS);
				if (pos >= 0) {
					// Grab the first field.
					msh = hl7Message.substring(0, pos);
					parts = msh.split("\\|");
				} else {
					msh = null;
					parts = new String[0];
				}
			}
		}
		
		public String getField(int index) {
			if (index < 0) {
				throw new IllegalArgumentException("Field number must be positive");
			}
			if (parts == null) {
				return null;
			}
			if (index >= parts.length) {
				return null;
			}
			return parts[index];
		}
		
		public String getFirstSubFieldOf(int index) {
			if (index < 0) {
				throw new IllegalArgumentException("Field number must be positive");
			}
			if (parts == null) {
				return null;
			}
			if (index >= parts.length) {
				return null;
			}
			return StringUtils.substringBefore(parts[index], "~");
		}
	}

	private enum ParseState {
		CANNOT_START_SEGMENT,
		CAN_START_SEGMENT,
		WITHIN_SEGMENT,
		END_SEGMENT_NAME,
		CHOMP_TO_DELIMITER
	}
	/**
	 * Mash PHI in segments
	 * This function ensures that HL7 message content potentially containing PHI is removed from the message.
	 * Sadly, some systems report detailed message content in fault responses.
	 *  
	 * MSH|^~\\&|WebIZ.1.0.9035.29972|PW0000|AS0000|AS0000|20241116123011.989+0900||RSP^K11^RSP_K11|PW000020241116301198|P|2.5.1|||NE|NE|||||Z33^CDCPHINVS\r
	 * MSA|AE|AS000020241115301090\rERR||MSH^1^6^1^1|999^ApplicationError^HL70357|E|4^Invalid value^HL70533^WEBIZ-AUTH-625^Facility is inactive or suspended^L||,  
	 * Next Diagnostic: |Facility is inactive or suspended.,  Next Message: One or more errors/warnings occured that may effect query results.\r
	 * QAK|755718|AE|Z34^Request Immunization History^CDCPHINVS\r
	 * QPD|Z34^Request Immunization History^CDCPHINVS|755718|84579^^^AS0000^MR~000000002^^^PW0000^SR|SIMPSON^BART^M^^^^L||19990101|M|2000 AS ST^^AENKAN^AS^96960^^P|^NET^X.400^BERSERY-KEMP@ENVISIONTECHNOLOGY.COM~^PRN^PH^^^864^1309701|N\r
	 * ZSA|AF^Application Fail - Message Failed to Execute. Generally this occurs for critical errors, which preve...^ENV0008|1853^^^PW0000^HL7LogIdIncomming~701265^^^PW0000^WebServiceLogIdIncomming~AS000020241115301090^^^AS0000^MessageControlId\r
	 *
	 * @param message	The message that may contain an HL7 Segment containing PHI in it
	 * @return	The masked message
	 */
	public static String maskSegments(String message) {
		// Identify segment starts in text messages using the case sensitive pattern /[^a-zA-Z0-9][A-Z]{3}\|/
		// (a non-alphanumeric character, followed by three uppercase alphabetic characters followed by a vertical bar |).
		// identify segment ends at either \r or \n or next segment start.
		if (message == null) {
			return null;
		}
		StringReader r = new StringReader(message);
		ParseState state = ParseState.CAN_START_SEGMENT;	// Can start segment delimiter
		StringBuilder save = new StringBuilder();
		StringBuilder b = new StringBuilder();
		int cc;
		try {
			while ((cc = r.read()) != -1) {
				char c = (char) cc;
				state = transitionState(state, save, b, c);
			}
		} catch (IOException e) {
			// Never happens
		}
		if (!save.isEmpty()) {
			b.append(save);
		}
		return b.toString();
	}

	private static ParseState transitionState( // NOSONAR (Sonar hates state machines)
		ParseState state, 
		StringBuilder save, 
		StringBuilder b, 
		char c
	) {  
		switch (state) {
		case CANNOT_START_SEGMENT: // Not ready to start segment
			b.append(c);
			if (!Character.isLetterOrDigit(c)) {
				return ParseState.CAN_START_SEGMENT;
			}
			return state;
		case CAN_START_SEGMENT: // Ready to start segment
			if (Character.isLetterOrDigit(c)) {
				save.setLength(0);
				save.append(c);
				return ParseState.WITHIN_SEGMENT;	// Started segment delimiter
			} 
			b.append(c);
			return state;
		case WITHIN_SEGMENT: // within a segment
			save.append(c);
			if (Character.isLetterOrDigit(c)) {
				if (save.length() == 3) {
					return ParseState.END_SEGMENT_NAME;
				}
				return ParseState.WITHIN_SEGMENT;
			} 
			b.append(save.toString());
			save.setLength(0);
			return ParseState.CAN_START_SEGMENT;
		case END_SEGMENT_NAME: // waiting for |
			save.append(c);
			String seg = save.toString();
			save.setLength(0);
			b.append(seg);
			if (c == '|') {
				seg = seg.substring(0, 3);
				if (HL7Utils.DEFAULT_ALLOWED_SEGMENTS.containsKey(seg)) {
					// These segments don't contain PHI and so can be passed through.
					return ParseState.CAN_START_SEGMENT;
				}
				b.append("...[masked]...");
				return ParseState.CHOMP_TO_DELIMITER;
			} 
			if (Character.isLetterOrDigit(c)) {
				return ParseState.CANNOT_START_SEGMENT;
			}
			return ParseState.CAN_START_SEGMENT;
		case CHOMP_TO_DELIMITER: // within a segment until \n or \r
			if (c == '\n' || c == '\r') {
				b.append(c);
				return ParseState.CAN_START_SEGMENT;
			}
			return state;
		default:
			return state;
		}
	}
}
