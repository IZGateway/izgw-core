package gov.cdc.izgateway.soap.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Schema(description = "The response to a Connectivity Test request")
@Data
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
public class ConnectivityTestResponse extends SoapMessage implements HasEchoBack, SoapMessage.Response {
	private static final long serialVersionUID = 1L;
	@Schema(description = "The original message")
	private String echoBack;
	
	public ConnectivityTestResponse(SoapMessage that, String schema, boolean isUpgradeOrSchemaChange) {
		super(that, schema, isUpgradeOrSchemaChange);
		if (that instanceof HasEchoBack eb) {
			this.echoBack = eb.getEchoBack();
		}
	}
}
