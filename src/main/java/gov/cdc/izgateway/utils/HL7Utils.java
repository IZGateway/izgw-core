package gov.cdc.izgateway.utils;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;
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
	 * @param b             The string builder to copy the result to
	 * @param segment       The segment to strip.
	 * @param allowedFields The set of allowed fields.
	 * @returns The stripped segment
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
}
