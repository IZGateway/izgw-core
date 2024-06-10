package gov.cdc.izgateway.configuration;

import java.net.URL;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "server")
@Data
public class ServerConfiguration implements InitializingBean {
	@Schema(description = "The server hostname (which should be found in a a certificate having a private key in the keystore)")
	private String hostname = "dev.izgateway.org";

	@Schema(description = "The server port (typically 443 for https and 9081 for http)")
	private int port = 8080;

	@Schema(description = "The server protocol (typically https, but http may be used for some testing)")
	private String protocol = "http";

	@Schema(description = "The baseUrl to use for the server. This is computed after other properties are set, which will trash any configured value")
	private URL baseUrl;

	@Schema(
		description = "The mode of operation for the server (production or development).  Production servers will NOT log any PHI.  Development servers"
				+ "may log it for diagnostic testing. The onboarding server typically operates in development mode.", 
		pattern="prod|dev"
	)
	private String mode = "prod";

	@Override
	public void afterPropertiesSet() throws Exception {
		baseUrl = new URL(protocol, hostname, port, "/");
	}
}
