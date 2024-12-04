package gov.cdc.izgateway.logging.info;

import java.io.Serial;
import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.auth.x500.X500Principal;

import com.fasterxml.jackson.annotation.JsonProperty;

import gov.cdc.izgateway.utils.X500Utils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

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
    @Serial
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
        this.protocol = that.protocol;
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
                setCertificate(certs[0]);
                setCipherSuite(conx.getCipherSuite());
                setConnected(true);
            } catch (SSLPeerUnverifiedException | IllegalStateException ex) {
                // Ignore this.
            }
        }
    }

    public void setCertificate(X509Certificate cert) {
        if (cert == null) {
            commonName = null;
            organization = null;
            validFrom = null;
            validTo = null;
            serialNumber = null;
            serialNumberHex = null;
            return;
        }
        X500Principal subject = cert.getSubjectX500Principal();
        Map<String, String> parts = X500Utils.getParts(subject);

        commonName = parts.get(X500Utils.COMMON_NAME);

        // Get organization, and if not present, look for the Organizational Unit
        String o = parts.get(X500Utils.ORGANIZATION);
        if (StringUtils.isBlank(o)) {
            o = parts.get(X500Utils.ORGANIZATION_UNIT);
        }
        organization = o;
        validFrom = cert.getNotBefore();
        validTo = cert.getNotAfter();
        serialNumber = String.valueOf(cert.getSerialNumber());
        serialNumberHex = cert.getSerialNumber().toString(16);
    }

}