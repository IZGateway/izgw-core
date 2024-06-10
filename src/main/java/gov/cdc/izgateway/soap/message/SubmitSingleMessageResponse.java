package gov.cdc.izgateway.soap.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Schema(description="A submitSingleMessage request")
@Data
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
public class SubmitSingleMessageResponse extends SoapMessage implements HasHL7Message, SoapMessage.Response {
	private static final long serialVersionUID = 1L;
	@Schema(description="The message to sent back from the destination")
	protected String hl7Message;
	@Schema(description="True if the HL7 Message is CDATA wrapped, false otherwise")
	private boolean isCdataWrapped;

	public SubmitSingleMessageResponse(SoapMessage that, String schema, boolean isUpgradeOrSchemaChange) {
		super(that, schema, isUpgradeOrSchemaChange);
		if (that instanceof HasHL7Message hl7m) {
			hl7Message = hl7m.getHl7Message();
		}
	}
	public SubmitSingleMessageResponse(String hl7Message) {
		this();
		this.hl7Message = hl7Message;
	}
}
