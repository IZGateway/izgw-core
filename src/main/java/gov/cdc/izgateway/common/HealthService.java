package gov.cdc.izgateway.common;

import gov.cdc.izgateway.logging.event.Health;
import gov.cdc.izgateway.logging.markers.Markers2;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HealthService {
	private HealthService() {}
	private static Health health = new Health();

   //overloaded setHealthy method  that is invoked when there is an exception
    public static void setHealthy(Throwable ex) {
        health.setLastException(ex);
        setHealthy(false, ex.getMessage());
    }

    public static void setHealthy(boolean b, String message) {
    	health.setHealthy(b);
    	health.setLastChangeReason(message);
        log.info(Markers2.append("health", health), "Server health changed to {}", health.isHealthy() ? "healthy" : "not healthy");
    }

	public static Health getHealth() {
        return health.copy();
    }
    
	public static void incrementVolumes(boolean hasProcessError) {
		health.incrementRequestVolume();
		if (!hasProcessError) {
			health.incrementSuccessVolume();
		}
	}
	public static void setServerName(String serverName) {
		health.setServerName(serverName);
	}
	public static void setBuildName(String build) {
		health.setBuildName(build);
	}

	public static void setDatabase(String url) {
		if (health.getDatabase() == null) {
			health.setDatabase(url);
		} else {
			health.setDatabase(health.getDatabase() + ", " + url);
		}
	}
	
	/**
	 * Set the egress DNS address for this server
	 * @param address	The egress DNS address
	 */
	public static void setEgressDnsAddress(String address) {
		health.setEgressDnsAddress(address);
	}
	
	/**
	 * Set the ingress DNS address for this server
	 * @param address	The ingress DNS address
	 */
	public static void setIngressDnsAddress(String[] address) {
		health.setIngressDnsAddress(address);
	}
	
}