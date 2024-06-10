package gov.cdc.izgateway.logging.info;

import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import gov.cdc.izgateway.soap.message.SoapMessage;
import gov.cdc.izgateway.soap.message.WsaHeaders;
import gov.cdc.izgateway.soap.net.SoapMessageWriter;
import gov.cdc.izgateway.utils.FixedByteArrayOutputStream;
import gov.cdc.izgateway.utils.IndentingXMLStreamWriter;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Records a Web Service message to or from an endpoint")
@Data
public class MessageInfo {
	@Schema(description = "Records request Web Service messages to an endpoint")
	@Data
	public static class RequestInfo {
		private MessageInfo ws_request_message; 		// NOSONAR This name is kept for log compatibility
	}	
	@Schema(description = "Records response Web Service messages from an endpoint")
	@Data
	public static class ResponseInfo {
		private MessageInfo ws_response_message; 		// NOSONAR This name is kept for log compatibility
	}	
	
	@Schema(description = "The message payload")
	@Getter(AccessLevel.NONE)
	private final SoapMessage payload;
	
	@Schema(description = "The Http Headers associated with the message")
	@JsonInclude(JsonInclude.Include.NON_NULL) 
	private Map<String, List<String>> httpHeaders;
	
	public enum Direction {
		INBOUND,
		OUTBOUND
	}
	@Schema(description = "The direction of the message")
	private final Direction direction;

	public enum EndpointType {
		CLIENT,
		SERVER
	}
	@Schema(description = "The type of endpoint")
	private final EndpointType endpointType;
	
	@JsonIgnore
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private String payloadString;
	
	private FaultInfo soapFault;
	
	private final WsaHeaders soapHeaders;

	private final boolean filtering;
	
	public String getPayload() {
		if (StringUtils.isNotEmpty(payloadString)) {
			return payloadString;
		}
		
		// Generate payload from the object itself.
		FixedByteArrayOutputStream fos = new FixedByteArrayOutputStream();
		try {
			new SoapMessageWriter(payload, IndentingXMLStreamWriter.createInstance(fos), filtering).write();
		} catch (XMLStreamException e) {
			// Swallow this, we only want the first part anyway.
		}
		payloadString = fos.toString();
		return payloadString;
	}
	
	public MessageInfo(SoapMessage payload, EndpointType endpointType, Direction direction, boolean filtering) {
		this.filtering = filtering;
		this.direction = direction;
		this.endpointType = endpointType;
		this.payload = payload;
		this.soapHeaders = payload == null ? null : payload.getWsaHeaders();
	}
}