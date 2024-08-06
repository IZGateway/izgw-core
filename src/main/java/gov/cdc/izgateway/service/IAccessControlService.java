package gov.cdc.izgateway.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.web.bind.annotation.RequestMethod;

import gov.cdc.izgateway.model.IAccessControl;

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
	/** Defines an endpoint that has OPEN access to any user */
	static final String OPEN_TO_ANY = "OPEN";

	/** Get the server name 
	 * @return The server name
	 */
	String getServerName();

	/** Refresh access control data */
	void refresh();

	/** @return Get mapping of users to roles */
	Map<String, TreeSet<String>> getUserRoles();

	/** @return Get mapping users in groups */
	Map<String, Map<String, Boolean>> getAllowedUsersByGroup();

	/** @return Get allowed routes for each event (submission) type */
	Map<String, Map<String, Boolean>> getAllowedRoutesByEvent();

	/**
	 * Checks roles for a user
	 * @param user	The user (certificate common name)
	 * @param role	The role
	 * @return	true if user is permitted in the given role
	 */
	boolean isUserInRole(String user, String role);

	/**
	 * Checks to see if a user is blacklisted
	 * @param user	The user
	 * @return	true if the user has been blacklisted
	 */
	boolean isUserBlacklisted(String user);

	/**
	 * @param event	The event (submission) type.
	 * @return	A map of routes to whether or not they are allowed for this event.
	 */
	Map<String, Boolean> getEventMap(String event);

	/**
	 * @return the list of event (submission) types
	 */
	Set<String> getEventTypes();

	/**
	 * Returns true of the route is allowed for the specified event (submission) type
	 * @param route	The route (a DEX endpoint)
	 * @param event The event
	 * @return True if the event can be sent to the specified route
	 */
	boolean isRouteAllowed(String route, String event);

	/**
	 * Set the server name.  Present to support testing.
	 * @param serverName	The name of the server.
	 */
	void setServerName(String serverName);

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
	boolean isMemberOf(String user, String group);

	/**
	 * Removes a user from the blacklist.  Only used for blacklisting at this point
	 * INSIDE IZGW, and not suitable for general user/group membership management.
	 * @param user	The user
	 * @return	The access control entry
	 */
	IAccessControl removeUserFromBlacklist(String user);

	/**
	 * Add a user to the blacklist. Only used for blacklisting at this point INSIDE IZGW,
	 * and not suitable for general user/group membership management
	 * @param user	The user
	 * @return	The new access control entry.
	 */
	IAccessControl addUserToBlacklist(String user);

}