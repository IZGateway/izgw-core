package gov.cdc.izgateway.model;

import java.util.Date;

/**
 * Represents a user that is allowed to send to a destination endpoint.
 * <p>
 * This interface provides access to the destination ID, environment, principal name,
 * and enabled status for access control.
 * </p>
 * @author Audacious Inquiry
 */
public interface IAllowedUser extends DbAudit, HasEnvironment, Comparable<IAllowedUser> {
    /**
     * Gets the name of the destination that is permitting a principal to send to it.
     *
     * @return the destination ID
     */
    String getDestinationId();

    /**
     * Sets the name of the destination that is permitting a principal to send to it.
     *
     * @param destinationId the destination ID
     */
    void setDestinationId(String destinationId);

    /**
     * Gets the environment where access is being permitted (e.g., onboarding, production).
     *
     * @return the environment
     */
    int getEnvironment();

    /**
     * Sets the environment where access is being permitted.
     *
     * @param environment the environment
     */
    void setEnvironment(int environment);

    /**
     * Gets the name of the principal that is permitted to send to the endpoint.
     *
     * @return the principal name
     */
    String getPrincipal();

    /**
     * Sets the name of the principal that is permitted to send to the endpoint.
     *
     * @param principal the principal name
     */
    void setPrincipal(String principal);

    /**
     * Gets whether access is enabled.
     *
     * @return true if access is enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Sets whether access is enabled.
     *
     * @param enabled true to enable access, false to disable
     */
    void setEnabled(boolean enabled);

	/**
	 * Gets the validatedOn date
	 * @return the validatedOn date
	 */
	Date getValidatedOn();
	/**
	 * Sets the validatedOn date
	 * @param validatedOn the validatedOn date
	 */
	void setValidatedOn(Date validatedOn);
	
    default int compareTo(IAllowedUser that) {
    	int value = Integer.compare(getEnvironment(), that.getEnvironment());
    	if (value != 0) {
			return value;
		}
    	value = getDestinationId().compareTo(that.getDestinationId());
    	if (value != 0) {
			return value;
		}
    	value = this.getPrincipal().compareTo(that.getPrincipal());
    	if (value != 0) {
			return value;
		}
    	return Boolean.compare(this.isEnabled(), that.isEnabled());
    }
}
