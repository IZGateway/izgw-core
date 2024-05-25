package gov.cdc.izgateway.soap.message;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description="The Web Services Addressing headers in a SOAP message. See https://www.w3.org/submissions/2004/SUBM-ws-addressing-20040810/")
public class WsaHeaders implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String WSA_NS = "http://www.w3.org/2005/08/addressing";
	public static final String WSA_PREFIX = "wsa";

	@Schema(description="The Web Service Action to perform")
	@JsonProperty("Action")
	private String action;

	@Schema(description="The identifier of this message")
	@JsonProperty("MessageID")
	private String messageID = getRandomID(); // Set a default value
	
	@JsonProperty("From")
	@Schema(description="The identifier of the sender of this message")
	private String from;

	@JsonProperty("To")
	@Schema(description="The destination of this message")
	private String to = "http://www.w3.org/2005/08/addressing/anonymous";
	
	@JsonProperty("RelatesTo")
	@Schema(description="The identifier of the message this message is a response to")
	private String relatesTo;

	private static List<Pair<String, Function<SoapMessage, String>>> pairs = Arrays.asList(
			Pair.of("Action", (Function<SoapMessage, String>)(m -> m.getWsaHeaders().getAction())),
			Pair.of("MessageID", (Function<SoapMessage, String>)(m -> m.getWsaHeaders().getMessageID())),
			Pair.of("From", (Function<SoapMessage, String>)(m -> m.getWsaHeaders().getFrom())),
			Pair.of("To", (Function<SoapMessage, String>)(m -> m.getWsaHeaders().getTo())),
			Pair.of("RelatesTo", (Function<SoapMessage, String>)(m -> m.getWsaHeaders().getRelatesTo()))
		);
		
	public static List<Pair<String, Function<SoapMessage, String>>> getKeyValueSuppliers() {
		return pairs;
	}

	private static String getRandomID() {
		return UUID.randomUUID().toString(); 
	}
	
	@JsonIgnore
	public boolean isEmpty() {
		return StringUtils.isAllEmpty(action, messageID, to, relatesTo, from);
	}
	
	WsaHeaders copyFrom(WsaHeaders that) {
		this.action = that.action;
		this.messageID = that.messageID;
		this.from = that.from;
		this.to = that.to;
		this.relatesTo = that.relatesTo;
		return this;
	}

	void respondingTo(WsaHeaders that) {
		this.relatesTo = that.messageID;
		// Assign a new message ID if the two match.  Message IDs are intended to be unique.
		if (this.messageID.equals(that.messageID)) {
			this.messageID = getRandomID();
		}
	}
}