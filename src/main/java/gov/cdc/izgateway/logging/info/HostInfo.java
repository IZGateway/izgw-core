package gov.cdc.izgateway.logging.info;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import gov.cdc.izgateway.logging.markers.Markers2;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The HostInfo record records the information that can be known about a host without connecting to it
 * or being connected to it.  That's basically the DNS name (host) and IP Address. 
 */
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostInfo {
    public static final String UNKNOWN_IP_ADDRESS = "unknown";
    public static final String LOCALHOST = "localhost";
    public static final String LOCALHOST_IP4 = "127.0.0.1";
	public static final String LOCALHOST_IP6 = "::1";

	@Schema(description="The name of the host associated with the endpoint.")
    @JsonProperty
	protected String host;
    
    @Schema(description="The IP address of the host associated with the endpoint.")
    @JsonProperty
    protected String ipAddress;
    
    @JsonIgnore
    protected void setHostAndIpAddressFromUrl(URL url) {
    	try {
            if (url.getHost() == null) {
                setHost(LOCALHOST);
                setIpAddress(LOCALHOST_IP4);
                return;
            } else {
                setHost(url.getHost());
            }
			InetAddress addr = InetAddress.getByName(getHost());
			ipAddress = addr.getHostAddress();
		} catch (UnknownHostException e) {
			setAddressUnknown();
		}
    }
    
    @JsonIgnore
    protected void setHostAndIpAddressFromUrl(String url) {
    	if (StringUtils.isEmpty(url)) {
    		setHost(null);
    		setIpAddress(null);
    		return;
    	}
    	try {
    		if (url.startsWith("/")) {
    			setHost(LOCALHOST);
    			setIpAddress(LOCALHOST_IP4);
    			return;
    		}
    		setHostAndIpAddressFromUrl(new URL(url));
    	} catch (MalformedURLException ex) {
            log.error(Markers2.append(ex), "Malformed URL: {}", url, ex);
			setAddressUnknown();
    	}
    }
    @JsonIgnore
    public void setAddressUnknown() {
		setHost(UNKNOWN_IP_ADDRESS);
		setIpAddress(UNKNOWN_IP_ADDRESS);
    }
}