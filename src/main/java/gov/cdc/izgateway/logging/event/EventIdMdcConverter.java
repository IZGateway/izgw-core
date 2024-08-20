package gov.cdc.izgateway.logging.event;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class EventIdMdcConverter extends ClassicConverter {
    public static final String EVENT_ID_MDC_KEY = "eventId";

    private static final String SECTION_PREFIX = " [";
    private static final String SECTION_SUFFIX = "]";

    @Override
    public String convert(ILoggingEvent event) {
        Map<String, String> mdcProps = event.getMDCPropertyMap();

        return (mdcProps.containsKey(EVENT_ID_MDC_KEY) ? (SECTION_PREFIX + mdcProps.get(EVENT_ID_MDC_KEY) + SECTION_SUFFIX) : StringUtils.EMPTY);
    }
}
