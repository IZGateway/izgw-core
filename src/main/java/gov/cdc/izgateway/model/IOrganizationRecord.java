package gov.cdc.izgateway.model;

import java.util.Set;

/**
 * Represents an organization record with audit information and principal names.
 * This interface provides methods to manage the organization name and principal names
 * for an organization record.
 *
 * @author Audacious Inquiry
 */
public interface IOrganizationRecord extends DbAudit {
	/**
	 * Returns the set of principal names associated with the organization record.
	 *
	 * @return a set of principal names
	 */
	Set<String> getPrincipalNames();

	/**
	 * Adds a principal name to the organization record.
	 *
	 * @param principalName the principal name to add
	 * @return true if the list was modified, false otherwise
	 */
	boolean addPrincipalName(String principalName);

	/**
	 * Removes a principal name from the organization record.
	 *
	 * @param principalName the principal name to remove
	 * @return true if the list was modified, false otherwise
	 */
	boolean removePrincipalName(String principalName);

	/**
	 * Gets the organization name.
	 *
	 * @return the organization name
	 */
	String getOrganizationName();

	/**
	 * Sets the organization name.
	 *
	 * @param organizationName the organization name to set
	 */
	void setOrganizationName(String organizationName);

	/**
	 * Gets the type of the organization (e.g., "IIS", "Provider", "Consumer").
	 * @return	The organization type
	 */
	String getType();
	/**
	 * Sets the type of the organization (e.g., "IIS", "Provider", "Consumer").
	 * @param type
	 */
	void setType(String type);
}