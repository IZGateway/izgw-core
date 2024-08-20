package gov.cdc.izgateway.utils;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Terser;
import ca.uhn.hl7v2.validation.ValidationContext;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import org.apache.commons.lang3.StringUtils;

public class HL7MessageFields {
    private static final HapiContext HAPI_CONTEXT;
    static {
        HAPI_CONTEXT = new DefaultHapiContext();

        ValidationContext noValidation = ValidationContextFactory.noValidation();
        HAPI_CONTEXT.setValidationContext(noValidation);
    }

    private static final String UNKNOWN_VALUE = "{unknown}";

    private enum ParsedPath {
        FIELD_SEPARATOR("/MSH-1", 1),
        ENCODING_CHARACTERS("/MSH-2", 2),
        SENDING_APPLICATION("/MSH-3", 3),
        SENDING_FACILITY("/MSH-4", 4),
        RECEIVING_APPLICATION("/MSH-5", 5),
        RECEIVING_FACILITY("/MSH-6", 6),
        MESSAGE_DATETIME("/MSH-7", 7),
        SECURITY("/MSH-8", 8),
        MESSAGE_TYPE("/MSH-9-1", 9, true),
        SENDING_RESPONSIBLE_ORGANIZATION("/MSH-22", 22);

        private String path;
        private int fallbackLocation;
        ParsedPath(String path, int hl7FallbackLocation) {
            this(path, hl7FallbackLocation, false);
        }
        ParsedPath(String path, int hl7FallbackLocation, boolean isFirstComponentOnly) {
            this.path = path;
            if (hl7FallbackLocation == 0) {
                hl7FallbackLocation = Integer.MAX_VALUE;
            } else {
                hl7FallbackLocation--; // adjust for HL7 Offset by 1
            }
            // Flag those requesting only first component
            this.fallbackLocation = isFirstComponentOnly ? -fallbackLocation: fallbackLocation;
        }

        /**
         * @return the path
         */
        public String getPath() {
            return path;
        }
        /**
         * @return the fallbackLocation
         */
        public int getFallbackLocation() {
            return fallbackLocation >= 0 ? fallbackLocation : -fallbackLocation;
        }

        public boolean isFirstComponentOnly() {
            return fallbackLocation < 0;
        }

        public boolean hasFallbackLocation() {
            return fallbackLocation != Integer.MAX_VALUE;
        }
    }

    private final PipeParser parser;
    /** Fallback on parsing error to string splitting on | */
    private String[] fallbackFields;
    private String[] parsedFields = new String[ParsedPath.values().length];
    private Terser messageFields;
    private String errorMessage;
    private boolean parseError;

    public HL7MessageFields() {
        this(HAPI_CONTEXT, null);
    }
    public HL7MessageFields(HapiContext hapiContext) {
        this(hapiContext, null);
    }
    public HL7MessageFields(String hl7Message) {
        this(HAPI_CONTEXT, hl7Message);
    }
    public HL7MessageFields(HapiContext hapiContext, String hl7Message) {
        parser = (hapiContext == null ? HAPI_CONTEXT : hapiContext).getPipeParser();
        if (hl7Message != null) {
            parseHL7MessageToFields(hl7Message);
        }
    }

    public boolean parseHL7MessageToFields(String hl7Message) {
        try {
            messageFields = new Terser(parser.parse(hl7Message));
        } catch (HL7Exception e) {
            parseError = true;
            errorMessage = e.getMessage();
            fallbackFields = hl7Message.split("[\\r\\n]")[0].split("\\|", 23);
        }

        setParsedValues();
        return !parseError;
    }

    private void setParsedValues() {
        for (ParsedPath path : ParsedPath.values()) {
            try {
                parsedFields[path.ordinal()] = messageFields.get(path.getPath());
            } catch (HL7Exception | NullPointerException e) {
                if (path.hasFallbackLocation()) {
                    parsedFields[path.ordinal()] = UNKNOWN_VALUE;
                } else {
                    String fallbackValue;
                    fallbackValue = fallbackFields[path.getFallbackLocation()];
                    if (path.isFirstComponentOnly()) {
                        fallbackValue = StringUtils.substringBefore(fallbackValue, "^");
                    }
                    parsedFields[path.ordinal()] = fallbackValue;
                }
            }
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isParseErrorError() {
        return parseError;
    }

    /**
     * @return the fieldSeparator
     */
    public String getFieldSeparator() {
        return parsedFields[ParsedPath.FIELD_SEPARATOR.ordinal()];
    }

    /**
     * @return the encodingCharacters
     */
    public String getEncodingCharacters() {
        return parsedFields[ParsedPath.ENCODING_CHARACTERS.ordinal()];
    }

    /**
     * @return the sendingApplication
     */
    public String getSendingApplication() {
        return parsedFields[ParsedPath.SENDING_APPLICATION.ordinal()];
    }

    /**
     * @return the sendingFacility
     */
    public String getSendingFacility() {
        return parsedFields[ParsedPath.SENDING_FACILITY.ordinal()];
    }

    /**
     * @return the receivingApplication
     */
    public String getReceivingApplication() {
        return parsedFields[ParsedPath.RECEIVING_APPLICATION.ordinal()];
    }

    /**
     * @return the receivingFacility
     */
    public String getReceivingFacility() {
        return parsedFields[ParsedPath.RECEIVING_FACILITY.ordinal()];
    }

    /**
     * @return the messageDateTime
     */
    public String getMessageDateTime() {
        return parsedFields[ParsedPath.MESSAGE_DATETIME.ordinal()];
    }

    /**
     * @return the securitySegment
     */
    public String getSecurity() {
        return parsedFields[ParsedPath.SECURITY.ordinal()];
    }

    /**
     * @return the messageType
     */
    public String getMessageType() {
        return parsedFields[ParsedPath.MESSAGE_TYPE.ordinal()];
    }

    /**
     * @return the sendingResponsibleOrganization
     */
    public String getSendingResponsibleOrganization() {
        return parsedFields[ParsedPath.SENDING_RESPONSIBLE_ORGANIZATION.ordinal()];
    }
}
