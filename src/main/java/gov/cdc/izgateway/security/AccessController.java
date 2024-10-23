package gov.cdc.izgateway.security;

import gov.cdc.izgateway.common.ResourceNotFoundException;
import gov.cdc.izgateway.model.IAccessControl;
import gov.cdc.izgateway.service.IAccessControlService;
import io.swagger.v3.oas.annotations.Operation;

import java.util.Collections;
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

@RestController
@CrossOrigin
@RolesAllowed({Roles.ADMIN})
@RequestMapping({"/rest"})
@Lazy(false)
public class AccessController {
	private final AccessControlRegistry registry;
	private final IAccessControlService service;
	
	@Autowired
	public AccessController(AccessControlRegistry registry, IAccessControlService service) {
		registry.register(this);
		this.registry = registry;
		this.service = service;
	}
	
	@Operation(summary="Report on access control settings", description="Return access control endpoints, groups, routes and users")
	@GetMapping("/access")
	public Map<String, Object> getAccess() {
		Map<String, Object> maps = new TreeMap<>();
		maps.put("Endpoints", getAccessEndpoints());
		maps.put("Groups", getAccessGroups());
		maps.put("Routes", getAccessRoutes());
		maps.put("Users", getAccessUsers());
		return maps;
	}

	@Operation(summary="Report on access control endpoints", description="Return the list of endpoints under access controls")
	@GetMapping("/access/endpoints")
	public Map<String, List<String>> getAccessEndpoints() {
		return registry.getControls();
	}

	@Operation(summary="Report on access control groups", description="Return the list of groups under access controls")
	@GetMapping("/access/groups")
	public Map<String, Map<String, Boolean>> getAccessGroups() {
		return service.getAllowedUsersByGroup();
	}

	@Operation(summary="Report on access control group members", description="Return the list of members for a given group")
	@GetMapping("/access/groups/{group}")
	public Set<String> getUsersInGroup(@PathVariable String group) {
		Map<String, Boolean> members = getAccessGroups().get(group);
		if (members == null) {
			if (Roles.BLACKLIST.equals(group)) {
				members = Collections.emptyMap();
			} else {
				throw new ResourceNotFoundException(String.format("Group %s not found.", group));
			}
		}
		TreeSet<String> users = new TreeSet<>();
		for (Map.Entry<String, Boolean> e : members.entrySet()) {
			if (Boolean.TRUE.equals(e.getValue())) {
				users.add(e.getKey());
			}
		}
		return users;
	}

	@Operation(summary="Report on users in blacklist", description="Return the list of blacklisted users")
	@GetMapping("/access/blacklist") Set<String> getBlacklistedUsers() {
		return getUsersInGroup(Roles.BLACKLIST);
	}
	
	@Operation(summary="Add a user to blacklist", description="Add the specified user to the blacklist")
	@PostMapping("/access/blacklist")
	public IAccessControl addUserToBlackList(@RequestParam String user) {
		return service.addUserToBlacklist(user);
	}
	
	@Operation(summary="Delete a user from the blacklist", description="Delete the specified user from the blacklist")
	@DeleteMapping("/access/blacklist")
	public IAccessControl removeUserFromBlackList(@RequestParam String user) {
		return service.removeUserFromBlacklist(user);
	}

	@Operation(summary="Report on routes under access controls", description="Return the list routes under access controls")
	@GetMapping("/access/routes")
	Map<String, Map<String, Boolean>> getAccessRoutes() {
		return service.getAllowedRoutesByEvent();
	}

	@Operation(summary="Report on users under access controls", description="Return the users under access controls")
	@GetMapping("/access/users")
	public Map<String, TreeSet<String>> getAccessUsers() {
		return service.getUserRoles();
	}
}