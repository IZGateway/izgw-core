package gov.cdc.izgateway.utils;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * SystemUtils answers questions about the system and server environment.
 * 
 * @author Audacious Inquiry
 *
 */
public class SystemUtils {

	private static final String HOSTNAME = hostName();
	private static final int DESTTYPE = destType();
	// (1,'PRODUCTION'),(2,'TEST'), (3, 'ONBOARD'), (4, 'STAGE'), (5, 'DEV')
	private static final int DESTTYPE_PROD = 1;
	private static final int DESTTYPE_TEST = 2;
	private static final int DESTTYPE_ONBOARD = 3;
	private static final int DESTTYPE_STAGE = 4;
	private static final int DESTTYPE_DEV = 5;
    protected static final String[] ENVIRONMENTS = { "UNKNOWN", "Production", "Testing", "Onboarding", "Staging", "Development" };
    protected static final String[] ENVIRONMENT_TAGS = { "unknown", "prod", "test", "onboard", "staging", "dev" };

    private SystemUtils() { }
    
    /**
     * Get the hostname.
     * @return	The hostname.
     */
	public static String getHostname() {
		return HOSTNAME;
	}
	
	/**
	 * Get the value that identifies the environment.
	 * @return The destination type.
	 */
	public static int getDestType() {
		return DESTTYPE;
	}

	/** 
	 * Dynamically get the value that identifies the hostname.
	 * Used during class initiation ONCE.
	 * 
	 * @return The hostname.
	 */
	private static String hostName() {
		String host = System.getenv("HOSTNAME"); 
        if (  StringUtils.isEmpty( host)) {
            host = System.getenv("COMPUTERNAME");

            if ( StringUtils.isEmpty( host)) {
                try {
                    host = java.net.InetAddress.getLocalHost().getHostName();
                } catch (java.net.UnknownHostException e) {
                    host = "HOSTNAME_UNKNOWN";
                }
            }
        }

        return host;
	}

	/**
	 * Dynamically get the value that identifies the environment.
	 * Used during class initiation ONCE.
	 * @return The environment id.
	 */
	private static int destType() {
		String tag = System.getenv("ELASTIC_ENV_TAG");
		if (tag == null) {
			tag = "DEV";
		} 
		switch (tag.toUpperCase()) {
		case "PROD":
			return DESTTYPE_PROD;
		case "ONBOARD":
			return DESTTYPE_ONBOARD;
		case "STAGING":
			return DESTTYPE_STAGE;
		case "TEST":
			return DESTTYPE_TEST;
		case "DEV": // Fall through
		default:
			return DESTTYPE_DEV;
		}
	}

	/**
	 * Convert environment to a human readable string
	 * @return a human readable string representation of the environment id
	 */
	public static String getDestTypeAsString() {
		return ENVIRONMENTS[DESTTYPE];
	}
	
	/**
	 * Convert environment to the tag used to identify it in logs.
	 * @return the elastic log tag for the environment.
	 */
	public static String getDestTag() {
		return ENVIRONMENT_TAGS[DESTTYPE];
	}
	
	/**
	 * Get All tags used to identify an environment in elastic.
	 * @return the elastic log tags for all known environments.
	 */
	public static String[] getDestTags() {
		return ENVIRONMENT_TAGS;
	}

	/**
	 * Convert the string back to an integer
	 * @param destType2	The string
	 * @return	The environment value it corresponds to
	 */
	public static int getDestTypeId(String destType2) {
		return Arrays.asList(SystemUtils.ENVIRONMENTS).indexOf(destType2);
	}

	/**
	 * Get the list of legitimate environment names
	 * @return the list of legitimate environment names
	 */
	public static List<String> getDestTypes() {
		return Arrays.asList(SystemUtils.ENVIRONMENTS).subList(1, SystemUtils.ENVIRONMENTS.length);
	}
}