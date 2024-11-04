package gov.cdc.izgateway.logging.info;

import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

import com.fasterxml.jackson.annotation.JsonProperty;

import gov.cdc.izgateway.logging.RequestContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * The DestinationInfo object records information about the endpoint to which message are intended to be or have been sent.
 */
@Schema(description = "Information about a destination endpoint")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class DestinationInfo extends EndPointInfo {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
    @Schema(description = "The URL of the destination")
    @JsonProperty
    private String url;

    @Schema(description = "Whether a connection was made to the destination")
    @JsonProperty
    private boolean connected = false;
    
    @Schema(description = "The protocol used to connect to the destination")
    @JsonProperty
    private String protocol;
    
    
    /**
     * Copy constructor
     * @param that  The object to copy
     */
    public DestinationInfo(DestinationInfo that) {
        super(that);
        this.url = that.url;
        this.connected = that.connected;
    }

    public void setUrl(String url) {
    	this.url = url;
        super.setHostAndIpAddressFromUrl(url);
    }

    @Override
	public String getName() {
		return getId() + "=" + super.getName();
	}
	
    public void setFromConnection(HttpURLConnection con) throws SSLPeerUnverifiedException {
        if (con instanceof HttpsURLConnection conx) {
            try {
                X509Certificate[] certs = (X509Certificate[]) conx.getServerCertificates();
                // TODO Paul - Principal related code
                // setCertificate(certs[0]);
                setPrincipal(RequestContext.getPrincipal());
                setCipherSuite(conx.getCipherSuite());
                setConnected(true);
            } catch (SSLPeerUnverifiedException | IllegalStateException ex) {
                // Ignore this.
            }
        }
    }
}