package gov.cdc.izgateway.model;

/**
 * Represents a jurisdiction in IZ Gateway, including audit information.
 * <p>
 * Provides methods to get and set jurisdiction details such as description, ID, name, and prefix.
 * </p>
 */
public interface IJurisdiction extends DbAudit{

	/**
	 * Gets the description of the jurisdiction.
	 * @return the description
	 */
	String getDescription();

	/**
	 * Gets the jurisdiction ID.
	 * @return the jurisdiction ID
	 */
	int getJurisdictionId();

	/**
	 * Gets the name of the jurisdiction.
	 * @return the name
	 */
	String getName();

	/**
	 * Gets the prefix for the jurisdiction.
	 * @return the prefix
	 */
	String getPrefix();

	/**
	 * Sets the description of the jurisdiction.
	 * @param description the description
	 */
	void setDescription(String description);

	/**
	 * Sets the jurisdiction ID.
	 * @param jurisdictionId the jurisdiction ID
	 */
	void setJurisdictionId(int jurisdictionId);

	/**
	 * Sets the name of the jurisdiction.
	 * @param name the name
	 */
	void setName(String name);

	/**
	 * Sets the prefix for the jurisdiction.
	 * @param prefix the prefix
	 */
	void setPrefix(String prefix);

}