package gov.cdc.izgateway.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.web.bind.annotation.RequestMethod;

import gov.cdc.izgateway.model.IAccessControl;

public interface IAccessControlService {

	String ROUTE_CATEGORY = "eventToRoute";
	String GROUP_CATEGORY = "group";
	String OPEN_TO_ANY = "OPEN";

	String getServerName();

	void refresh();

	Map<String, TreeSet<String>> getUserRoles();

	Map<String, Map<String, Boolean>> getAllowedUsersByGroup();

	Map<String, Map<String, Boolean>> getAllowedRoutesByEvent();

	boolean isUserInRole(String user, String role);

	boolean isUserBlacklisted(String user);

	Map<String, Boolean> getEventMap(String event);

	Set<String> getEventTypes();

	boolean isRouteAllowed(String route, String event);

	/**
	 * Set the server name.  Present to support testing.
	 * @param serverName	The name of the server.
	 */
	void setServerName(String serverName);

	List<String> getAllowedRoles(RequestMethod method, String path);

	Boolean checkAccess(String user, String method, String path);

	boolean isMemberOf(String user, String group);

	IAccessControl removeUserFromGroup(String user, String group);

	IAccessControl addUserToGroup(String user, String group);

}