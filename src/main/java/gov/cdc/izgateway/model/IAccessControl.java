package gov.cdc.izgateway.model;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents access control information for a member, category, and name, with audit details.
 * <p>
 * This interface provides methods to get and set access control properties, including whether access is allowed.
 * </p>
 */
public interface IAccessControl extends DbAudit{

	/**
	 * Gets the category of access control.
	 * @return the category
	 */
	String getCategory();

	/**
	 * Gets the member for access control.
	 * @return the member
	 */
	String getMember();

	/**
	 * Gets the name for access control.
	 * @return the name
	 */
	String getName();

	/**
	 * Returns whether access is allowed.
	 * @return true if access is allowed, false otherwise
	 */
	boolean isAllowed();

	/**
	 * Sets whether access is allowed.
	 * @param allowed true to allow access, false to deny
	 */
	void setAllowed(boolean allowed);

	/**
	 * Sets the category of access control.
	 * @param category the category
	 */
	void setCategory(String category);

	/**
	 * Sets the member for access control.
	 * @param member the member
	 */
	void setMember(String member);

	/**
	 * Sets the name for access control.
	 * @param name the name
	 */
	void setName(String name);

	/**
	 * Groups are simple names following the pattern [a-zA-Z0-9]+, and cannot be named "localhost".
	 * 
	 * @param	member	The pattern to check for a group name
	 * @return	True if this is a group name, false for a user name pattern.
	 */
	static boolean isGroup(String member) {
		return  !"localhost".equals(member) && member.matches("^[a-zA-Z0-9]+$");
	}

}