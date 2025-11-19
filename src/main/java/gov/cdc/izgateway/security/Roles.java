package gov.cdc.izgateway.security;

import java.util.List;

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
	private static final String[] values = {
		ADMIN, ADS, ADSPILOT, INTERNAL, BLACKLIST, OPERATIONS, SOAP, USERS	
	};
	// ADMIN user can use this header to avoid admin overrides.
	public static final String NOT_ADMIN_HEADER = "X-Not-Admin";
	/**
	 * Get the list of defined roles.
	 * 
	 * @return The list of defined roles
	 */
	public static List<String> values() {
		return List.of(values);
	}
	private Roles() {}
}
