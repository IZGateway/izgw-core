package gov.cdc.izgateway.security;

/**
 * Constants for use in @RolesAllowed annotation.
 */
public class Roles {
	public static final String ADMIN = "admin";
	public static final String ADS = "ads";
	public static final String ADSPILOT = "adspilot";
	public static final String INTERNAL = "internal";
	public static final String OPERATIONS = "operations";
	public static final String BLACKLIST = "blacklist";
	public static final String SOAP = "soap";
	public static final String USERS = "users";
	public static final String OPEN = "OPEN";
	private static final String[] values = {
		ADMIN, ADS, ADSPILOT, INTERNAL, BLACKLIST, OPERATIONS, SOAP, USERS, OPEN	
	};
	// ADMIN user can use this header to avoid admin overrides.
	public static final String NOT_ADMIN_HEADER = "X-Not-Admin";
	public static String[] values() {
		return values;
	}
	private Roles() {}
}
