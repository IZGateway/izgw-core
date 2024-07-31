package gov.cdc.izgateway.utils;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

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
	public static String getHostname() {
		return HOSTNAME;
	}
	
	public static int getDestType() {
		return DESTTYPE;
	}

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

	public static String getDestTypeAsString() {
		return ENVIRONMENTS[DESTTYPE];
	}
	
	public static String getDestTag() {
		return ENVIRONMENT_TAGS[DESTTYPE];
	}
	
	private static String getDestTag(int destType) {
		return ENVIRONMENT_TAGS[destType];
	}
	
	public static String[] getDestTags() {
		return ENVIRONMENT_TAGS;
	}

	public static int getDestTypeId(String destType2) {
		return Arrays.asList(SystemUtils.ENVIRONMENTS).indexOf(destType2);
	}

	public static List<String> getDestTypes() {
		return Arrays.asList(SystemUtils.ENVIRONMENTS).subList(1, SystemUtils.ENVIRONMENTS.length);
	}
}
