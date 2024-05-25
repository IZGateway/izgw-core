package gov.cdc.izgateway.soap.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Schema(description="A connectivity test request")
@Data
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
public class ConnectivityTestRequest extends SoapMessage implements HasEchoBack, HasCredentials {
	private static final long serialVersionUID = 1L;
	@Schema(description="The message to echo back")
	private String echoBack;
	@Schema(description="The Username for this request")
	private String username;
	@Schema(description="The Password for this request")
	private String password;

	/**
	 * Make a copy of an existing SoapMessage, possibly changing the schema. 
	 * @param that	The message to copy
	 * @param schema	The schema to use, or null to use the same schema.
	 * @param isUpgradeOrSchemaChange retained for constructor compatibiliity, but ignored
	 */
	public ConnectivityTestRequest(SoapMessage that, String schema, boolean isUpgradeOrSchemaChange) {
		super(that, schema, true);
		if (that instanceof HasEchoBack) {
			this.echoBack = ((HasEchoBack)that).getEchoBack();
		}
		if (that instanceof HasCredentials) {
			HasCredentials credentialed = (HasCredentials) that;
			this.username = credentialed.getUsername();
			this.password = credentialed.getPassword();
		}
	}
}
