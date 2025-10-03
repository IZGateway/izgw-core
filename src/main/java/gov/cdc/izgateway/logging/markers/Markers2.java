package gov.cdc.izgateway.logging.markers;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;
import org.springframework.core.annotation.AnnotationUtils;

import gov.cdc.izgateway.common.HasDestinationUri;
import gov.cdc.izgateway.soap.fault.Fault;
import net.logstash.logback.marker.EmptyLogstashMarker;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;

/**
 * Markers2 replaces the PhizLogstashMarkers class and is used to easily append markers
 * to a log message.  Markers are the principle way that JSON logs are written.
 * 
 * When the first object seen is a string, it is used as the name for the object that follows it.
 * When an unnamed object is seen, it's class name is used as the name for the object content.
 * 
 * Class Info x;
 * log.info(Markers2.append(x), "This is the message"); will generate JSON in the form:
 * 
 * { "info": { however Info is marshalled by Jackson }, "message": "This is the message" } 
 * 
 * And
 * log.info(Markers2.append("myInfo", x) "This is the message"); will generate JSON in the form:
 * 
 * { "myInfo": { however Info is marshalled by Jackson }, "message": "This is the message" }
 */
public class Markers2 {
    public static final String MARKER_FIELD_NAME_DELIM = "_";
    public static final String MARKER_FIELD_NAME_LITERAL_DELIMS = "-" + MARKER_FIELD_NAME_DELIM;
	private Markers2() {}
    
	public static LogstashMarker append(Object ...objects) {
		String fieldname;
		LogstashMarker marker = new EmptyLogstashMarker();
		
		for (int i = 0; i < objects.length; i++) {
			Object o = objects[i];
			if (o instanceof String s) {
				fieldname = s;
				o = (++i == objects.length) ? null : objects[i];  // NOSONAR Loop variable change OK here
				marker.add(Markers.append(fieldname, o));
			} else if (o instanceof LogstashMarker lm) {
				marker.add(lm);
			} else if (o instanceof Throwable t) {

            	StringWriter sw = new StringWriter();
            	PrintWriter w = new PrintWriter(sw);
            	t.printStackTrace(w);
            	
				marker.add(Markers.append("exception", t.getClass().getSimpleName()));
				marker.add(Markers.append("exceptionMessage", t.getMessage()));
				marker.add(Markers.append("stack_trace", sw.toString()));
				if (t instanceof Fault fault) {
					marker.add(Markers.append("faultName", fault.getFaultName()));
					marker.add(Markers.append("summary", fault.getSummary()));
					marker.add(Markers.append("detail", fault.getDetail()));
					marker.add(Markers.append("code", fault.getCode()));
					marker.add(Markers.append("retry", fault.getRetry()));
				}
				if (t instanceof HasDestinationUri hduri) {
					marker.add(Markers.append("destIId", hduri.getDestinationId()));
					marker.add(Markers.append("uri", hduri.getDestinationUri()));
				}
            	// If there is at least one cause
            	addCauses(marker, t);
			} else if (o != null) {
				fieldname = o.getClass().getSimpleName();
				marker.add(Markers.append(fieldname, o));
			}
		}
		return marker;
	}

	private static Throwable addCauses(LogstashMarker marker, Throwable t) {
		if (t.getCause() != null) {
			// Collect the causes in order
			List<String> l = new ArrayList<>();
			for (t = t.getCause(); t != null; t = t.getCause()) {
				l.add(String.format("%s: %s", t.getClass().getSimpleName(), t.getMessage()));
			}
			marker.add(Markers.append("cause", l));
		}
		return t;
	}
	
    public static String buildFieldName(Object markerObj) {
        Class<?> markerObjClass = markerObj.getClass();
        MarkerObjectFieldName markerObjFieldNameAnno = AnnotationUtils.findAnnotation(markerObjClass, MarkerObjectFieldName.class);
        String markerFieldName = markerObjFieldNameAnno != null ? 
        		markerObjFieldNameAnno.value() : markerObjClass.getSimpleName();
        return StringUtils.containsAny(markerFieldName, "-_") ? 
        		markerFieldName : CaseUtils.toCamelCase(markerFieldName, false);
    }

}
