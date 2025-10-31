package gov.cdc.izgateway.model;

import java.util.Date;

/**
 * Represents a deny list record for users blocked from accessing IZ Gateway.
 * <p>
 * Provides principal, environment, reason, denied date, and denied by information.
 * </p>
 */
public interface IDenyListRecord extends DbAudit{
    /**
     * Gets the name of the user to block (primary key).
     * @return the principal name
     */
    String getPrincipal();

    /**
     * Sets the name of the user to block.
     * @param principal the principal name
     */
    void setPrincipal(String principal);

    /**
     * Gets the environment the user is blocked from accessing.
     * @return the environment
     */
    Integer getEnvironment();

    /**
     * Sets the environment the user is blocked from accessing.
     * @param environment the environment
     */
    void setEnvironment(Integer environment);

    /**
     * Gets the reason the user was blocked.
     * @return the reason
     */
    String getReason();

    /**
     * Sets the reason the user was blocked.
     * @param reason the reason
     */
    void setReason(String reason);
}
