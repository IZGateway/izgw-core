package gov.cdc.izgateway.logging.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Marker;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import ch.qos.logback.classic.spi.ILoggingEvent;
import gov.cdc.izgateway.common.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.ObjectAppendingMarker;

/**
 * Class for documenting the LogEvent format and interfacing with the ILoggingEvent interface used by the
 * IZ Gateway logging system. The main purpose of this class is to support documentation in Swagger, rather
 * than marshalling between ILoggingEvent and the appenders that write the output (as Jackson could do that
 * by itself very easily). However, it also serves for that purpose as well.
 */
@Slf4j
@JsonPropertyOrder(
	{"@timestamp", "@version", "logger_name", "thread_name", "level", "level_value", "eventId", "sessionId", "message"}
)
@Schema(description="An entry in logs. Note: Log entries can have additional structured properties "
		+ " (e.g., Health or TransactionData).")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogEvent {
	private ILoggingEvent event;
	private Map<String, Object> properties;
	public LogEvent(ILoggingEvent event) {
		this.event = event;
		properties = new LinkedHashMap<>(event.getMDCPropertyMap());
		// The next two properties need to be removed because they are 
		// explicitly accessed through this interface.
		properties.remove("eventId");
		properties.remove("sessionId");
		getMarkers(event.getMarkerList(), properties);
	}
	
	@JsonProperty("@timestamp")
    @JsonFormat(shape=Shape.STRING, pattern = Constants.TIMESTAMP_FORMAT)
	@Schema(description="The timestamp of the log entry", 
			example="2023-12-01T19:24:24.416+0000",
			format=Constants.TIMESTAMP_FORMAT
	)
	public Date getTimestamp() {
		return new Date(event.getTimeStamp());
	}
	@Schema(description="The version of the log entry", example="1")
	@JsonProperty("@version") 
	public String getVersion() {
		return "1";
	}
	@Schema(description="The class logging this entry", example="gov.cdc.izgateway.Application")
	@JsonProperty("logger_name")
	public String getLoggerName() {
		return event.getLoggerName();
	}
	@Schema(description="The name of the thread logging this entry", example="IZ Gateway")
	@JsonProperty("thread_name") 
	public String getThreadName() {
		return event.getThreadName();
	}
	@Schema(description="The logging level of this entry",
		allowableValues= {"INFO", "WARN", "ERROR", "DEBUG", "TRACE" },
		example="INFO")
	@JsonProperty("level")
	public String getLevel() {
		return event.getLevel().toString();
	}
	@Schema(description="The logging level of this entry", example="20000")
	@JsonProperty("level_value") 
	public int getLevelValue() {
		return event.getLevel().toInt();
	}

	@Schema(description="The formatted message for the entry.", example="Heartbeat") 
	@JsonProperty("message")
	public String getMessage() {
		return event.getFormattedMessage();
	}
	@JsonAnyGetter
	public Map<String, Object> getProperties() {
		return properties;
	}
	
    @Schema(description="The common name on the certificate associated with the requester", example="dev.izgateway.org")
	public String getCommonName() {
		return event.getMDCPropertyMap().get("commonName");
	}
	
    @Schema(description="The URI for the request", example="/IISHubService")
	public String getRequestURI() {
		return event.getMDCPropertyMap().get("requestURI");
	}

	@Schema(description="The unique event of the request that generated this entry.", 
			example="34233194002.203", 
			pattern="\\d+\\.\\d+"
	)
	@JsonProperty("eventId")
	public 
	String getEventId() {
		return event.getMDCPropertyMap().get(EventId.EVENTID_KEY);
	}

	@Schema(description="The session id associated with the request that generated this entry.", example="0") 
	@JsonProperty("sessionId") 
	public String getSessionId() {
		return event.getMDCPropertyMap().get("sessionId");
	}

	// Insert other objects that may appear in log events
	@Schema(description="The health of the server (appears in Heartbeat messages)")
	public Health getHealth() {
		return (Health) properties.get("health");
	}
	
	@Schema(description="Metadata about the transaction (appears in TransactionData messages)")
	public TransactionData getTransactionData() {
		return (TransactionData) properties.get("transactionData");
	}
	
	public String toString() {
		if (event.getMarkerList() != null) {
			return event.toString() + " " + event.getMarkerList().toString();
		}
		return event.toString();
	}
	
	private Map<String, Object> getMarkers(Iterable<Marker> markers, Map<String, Object> map) {
		if (markers == null) {
			return map;
		}
		for (Marker marker: markers) {
			if (marker instanceof LogstashMarker l) {
				if (marker instanceof ObjectAppendingMarker o) {
					Object value = null;
					try {
						Method m = ObjectAppendingMarker.class.getDeclaredMethod("getFieldValue");
						m.setAccessible(true); // NOSONAR Intentional call to setAccessible
						value = m.invoke(o);
					} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						log.error("No such method available", e);
					}
					map.put(o.getFieldName(), value);
				}
				if (marker.hasReferences()) {
					getMarkers(l, map);
				}
			}
		}
		return map;
	}

}
