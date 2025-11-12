package gov.cdc.izgateway.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.web.bind.annotation.RequestMethod;

/**
 *	This class supports management of access controls in IZ Gateway.
 *
 * 	@author Audacious Inquiry
 */
public interface IAccessControlService {

	/** Defines which events can route to which endpoints */
	static final String ROUTE_CATEGORY = "eventToRoute";
	/** Defines a group member */
	static final String GROUP_CATEGORY = "group";

	/** Refresh access control data */
	void refresh();

	/** @return Get mapping of users to roles */
	Map<String, TreeSet<String>> getUserRoles();

	/** 
	 * Get groups
	 * The key is the group name.
	 * The value is an object describing the group.
	 * @return Get mapping of users to groups
     */
	Map<String, Object> getGroups();

	/**
	 * Returns true if the user is in the specified group
	 * @param user	The user
	 * @param group	The group
	 * @return	
	 */
	boolean isUserInGroup(String user, String group);
	
	/**
	 * Checks roles for a user
	 * @param user	The user (certificate common name)
	 * @param role	The role
	 * @return	true if user is permitted in the given role
	 */
	boolean isUserInRole(String user, String role);

	/**
	 * Checks to see if a user is on the DenyList
	 * @param user	The user
	 * @return	true if the user has been Denied access
	 */
	boolean isUserDenied(String user);

	/**
	 * @return the list of event (submission) types
	 */
	Set<String> getEventTypes();

	/**
	 * Get the roles allows to access a given method and path
	 * @param method	The HTTP method
	 * @param path	The URL path
	 * @return	The list of roles permitted to access the path and method
	 */
	List<String> getAllowedRoles(RequestMethod method, String path);

	/**
	 * Determine if a user can access a given path and method
	 * @param user	The user
	 * @param method	The HTTP method
	 * @param path	The URL path
	 * @return true if the user can access the specified method and path
	 */
	Boolean checkAccess(String user, String method, String path);

	/**
	 * Determines whether a user is a member of a given group.
	 * @param user	The user
	 * @param group	The group
	 * @return	True if use is a member of the group
	 */
	boolean canAccessDestination(String user, String group);

	/**
	 * Removes a user from the denylist.  Only used for deny listing at this point
	 * INSIDE IZGW, and not suitable for general user/group membership management.
	 * @param user	The user
	 * @return	The access control entry that was removed, or null if none existed.
	 */
	Object removeUserFromDenyList(String user);

	/**
	 * Add a user to the DenyList. Only used for denying at this point INSIDE IZGW,
	 * and not suitable for general user/group membership management
	 * @param user	The user
	 * @return	The new access control entry that was added.
	 */
	Object addUserToDenyList(String user);

	/**
	 * Get the list of deny listed users.
	 * @return The set of deny listed users.
	 */
	Set<String> getDenyList();

}