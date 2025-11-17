package gov.cdc.izgateway.security;

import gov.cdc.izgateway.common.ResourceNotFoundException;
import gov.cdc.izgateway.service.IAccessControlService;
import io.swagger.v3.oas.annotations.Operation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;

/**
 * REST controller for managing access control settings, endpoints, groups, routes, and users.
 * 
 * Endpoints are protected and require ADMIN role. Provides operations to report on and modify access controls,
 * including denylist management and group membership queries.
 * 
 * @author Audacious Inquiry
 */
@RestController
@CrossOrigin
@RolesAllowed({Roles.ADMIN})
@RequestMapping({"/rest"})
@Lazy(false)
public class AccessController {
	private final AccessControlRegistry registry;
	private final IAccessControlService service;
	
	/**
	 * Constructs the AccessController and registers it with the AccessControlRegistry.
	 *
	 * @param registry the access control registry
	 * @param service the access control service
	 */
	@Autowired
	public AccessController(AccessControlRegistry registry, IAccessControlService service) {
		registry.register(this);
		this.registry = registry;
		this.service = service;
	}
	
	/**
	 * Report on access control settings.
	 * 
	 * Returns access control endpoints, groups, routes, and users.
	 *
	 * @return a map containing endpoints, groups, routes, and users
	 */
	@Operation(summary="Report on access control settings", description="Return access control endpoints, groups, routes and users")
	@GetMapping("/access")
	public Map<String, Object> getAccess() {
		Map<String, Object> maps = new TreeMap<>();
		maps.put("migrated", service.isMigrated());
		maps.put("Endpoints", getAccessEndpoints());
		maps.put("Groups", getAccessGroups());
		maps.put("Routes", getAdsFileTypes());
		maps.put("Users", getAccessUsers());
		maps.put("DenyList", getDeniedUsers());
		return maps;
	}

	/**
	 * Report on access control endpoints.
	 * 
	 * Returns the list of endpoints under access controls.
	 *
	 * @return a map of endpoints and their controls
	 */
	@Operation(summary="Report on access control endpoints", description="Return the list of endpoints under access controls")
	@GetMapping("/access/endpoints")
	public Map<String, List<String>> getAccessEndpoints() {
		return registry.getApiEndpoints();
	}

	/**
	 * Report on access control groups.
	 * 
	 * Returns the list of groups under access controls.
	 *
	 * @return a map of group names to their allowed users
	 */
	@Operation(summary="Report on access control groups", description="Return the list of groups under access controls")
	@GetMapping("/access/groups")
	public Map<String, Object> getAccessGroups() {
		return service.getGroups();
	}

	/**
	 * Report on access control group members.
	 * <p>
	 * Returns the list of members for a given group.
	 *
	 * @param group the group name
	 * @return a set of users in the group
	 * @throws ResourceNotFoundException if the group is not found
	 */
	@Operation(summary="Report on access control group members", description="Return the list of members for a given group")
	@GetMapping("/access/groups/{group}")
	public Object getUsersInGroup(@PathVariable String group) {
		Object g = getAccessGroups().get(group);
		if (g == null) {
			throw new ResourceNotFoundException(String.format("Group %s not found.", group));
		}
		return g;
	}

	/**
	 * Report on users in deny list.
	 * <p>
	 * Returns the list of deny listed users.
	 *
	 * @return a set of deny listed users
	 */
	@Operation(summary="Report on users in deny list", description="Return the list of deny listed users")
	@GetMapping("/access/denylist") 
	public Set<String> getDeniedUsers() {
		return service.getDenyList();
	}
	
	/**
	 * Add a user to deny list.
	 * 
	 * Adds the specified user to the deny list.
	 *
	 * @param user the user to add
	 * @return the updated access control object
	 */
	@Operation(summary="Add a user to deny list", description="Add the specified user to the deny list")
	@PostMapping("/access/denylist")
	public Object addUserToDenyList(@RequestParam String user, @RequestParam(required=false) String reason) {
		return service.addUserToDenyList(user, reason);
	}
	
	/**
	 * Delete a user from the deny list.
	 * 
	 * Deletes the specified user from the deny list.
	 *
	 * @param user the user to remove
	 * @return the updated access control object
	 */
	@Operation(summary="Delete a user from the deny list", description="Delete the specified user from the deny list")
	@DeleteMapping("/access/denylist")
	public Object removeUserFromDenyList(@RequestParam String user) {
		return service.removeUserFromDenyList(user);
	}

	/**
	 * Returns the list of file types supported for ADS
	 *
	 * @return The set of file types that can be uploaded
	 */
	@Operation(summary="Report on file types", description="Return the list file types that can be uploaded for ADS")
	@GetMapping("/access/filetypes")
	Set<String> getAdsFileTypes() {
		return service.getEventTypes();
	}

	/**
	 * Report on users under access controls.
	 * <p>
	 * Returns the users under access controls.
	 *
	 * @return a map of user names to their roles
	 */
	@Operation(summary="Report on users under access controls", description="Return the users under access controls")
	@GetMapping("/access/users")
	public Map<String, TreeSet<String>> getAccessUsers() {
		return service.getUserRoles();
	}
}