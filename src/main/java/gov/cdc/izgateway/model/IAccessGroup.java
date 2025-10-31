package gov.cdc.izgateway.model;

import java.util.Date;
import java.util.List;

/**
 * Represents a group record for access control groups.
 * <p>
 * Provides group name, environment, description, roles, users, groups, and last changed date.
 * </p>
 */
public interface IAccessGroup extends DbAudit {
    /**
     * Gets the name for the group.
     * @return the group name
     */
    String getGroupName();

    /**
     * Sets the name for the group.
     * @param groupName the group name
     */
    void setGroupName(String groupName);

    /**
     * Gets the environment that the group is for.
     * @return the environment
     */
    Integer getEnvironment();

    /**
     * Sets the environment that the group is for.
     * @param environment the environment
     */
    void setEnvironment(Integer environment);

    /**
     * Gets the description of the group.
     * @return the description
     */
    String getDescription();

    /**
     * Sets the description of the group.
     * @param description the description
     */
    void setDescription(String description);

    /**
     * Gets the list of roles for the group.
     * @return the list of roles
     */
    List<String> getRoles();

    /**
     * Sets the list of roles for the group.
     * @param roles the list of roles
     */
    void setRoles(List<String> roles);

    /**
     * Gets the list of users allowed in the group.
     * @return the list of users
     */
    List<String> getUsers();

    /**
     * Sets the list of users allowed in the group.
     * @param users the list of users
     */
    void setUsers(List<String> users);

    /**
     * Gets the list of groups whose members are also members of this group.
     * @return the list of groups
     */
    List<String> getGroups();

    /**
     * Sets the list of groups whose members are also members of this group.
     * @param groups the list of groups
     */
    void setGroups(List<String> groups);
  
}
