package gov.cdc.izgateway.soap.message;

import org.apache.commons.lang3.StringUtils;

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
public class SubmitSingleMessageRequest extends SoapMessage implements HasHL7Message, HasFacilityID {
	private static final long serialVersionUID = 1L;
	@Schema(description="The message to send to the destination")
	protected String hl7Message;
	@Schema(description="The facility responsible for the message")
	private String facilityID;
	@Schema(description="The Username for this request")
	private String username;
	@Schema(description="The Password for this request")
	private String password;
	@Schema(description="True if the HL7 Message is CDATA wrapped, false otherwise")
	private boolean isCdataWrapped;

	public SubmitSingleMessageRequest(SoapMessage that, String schema, boolean isUpgradeOrSchemaChange) {
		super(that, schema, true);
		if (that instanceof HasHL7Message hl7) {
			this.hl7Message = hl7.getHl7Message();
		}
		if (that instanceof HasFacilityID hfid) {
			this.facilityID = hfid.getFacilityID();
		}
	}
	
	public SubmitSingleMessageRequest(String hl7Message) {
		this();
		setHl7Message(hl7Message);
	}
	
	public void setHl7Message(String message) {
		hl7Message = StringUtils.trim(message);
	}
}
