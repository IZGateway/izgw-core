package gov.cdc.izgateway.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * Pins the roles the registry resolves for {@link AccessController} endpoints (IGDD-3499).
 * <p>
 * The deny list and ADS file type endpoints are routine operational tasks and must stay
 * reachable by OPERATIONS in addition to ADMIN. Every other endpoint on this controller is
 * unchanged and must remain ADMIN-only, since that is the class-level default.
 */
class AccessControlRegistryRoleTests {
    private static final List<String> ADMIN_AND_OPERATIONS = List.of(Roles.ADMIN, Roles.OPERATIONS);
    private static final List<String> ADMIN_ONLY = List.of(Roles.ADMIN);

    private AccessControlRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AccessControlRegistry();
        registry.register(AccessController.class, "/rest");
    }

    @Test
    void denyListEndpointsAllowAdminAndOperations() {
        assertEquals(ADMIN_AND_OPERATIONS, registry.getAllowedRoles(RequestMethod.GET, "/rest/access/denylist"));
        assertEquals(ADMIN_AND_OPERATIONS, registry.getAllowedRoles(RequestMethod.POST, "/rest/access/denylist"));
        assertEquals(ADMIN_AND_OPERATIONS, registry.getAllowedRoles(RequestMethod.DELETE, "/rest/access/denylist"));
    }

    @Test
    void adsFileTypeEndpointAllowsAdminAndOperations() {
        assertEquals(ADMIN_AND_OPERATIONS, registry.getAllowedRoles(RequestMethod.GET, "/rest/access/filetypes"));
    }

    @Test
    void otherAccessControlEndpointsRemainAdminOnly() {
        assertEquals(ADMIN_ONLY, registry.getAllowedRoles(RequestMethod.GET, "/rest/access"));
        assertEquals(ADMIN_ONLY, registry.getAllowedRoles(RequestMethod.GET, "/rest/access/endpoints"));
        assertEquals(ADMIN_ONLY, registry.getAllowedRoles(RequestMethod.GET, "/rest/access/groups"));
        assertEquals(ADMIN_ONLY, registry.getAllowedRoles(RequestMethod.GET, "/rest/access/groups/*"));
        assertEquals(ADMIN_ONLY, registry.getAllowedRoles(RequestMethod.GET, "/rest/access/users"));
    }
}
