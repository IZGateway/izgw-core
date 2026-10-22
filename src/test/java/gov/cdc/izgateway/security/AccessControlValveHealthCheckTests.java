package gov.cdc.izgateway.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import gov.cdc.izgateway.logging.RequestContext;
import gov.cdc.izgateway.service.IAccessControlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Covers the unauthenticated bypass for infrastructure health probes (IGDD-3178).
 * <p>
 * The bypass is matched against an exact set of paths rather than a {@code /health} suffix, so
 * these tests pin both halves of that: the two Hub probe paths stay reachable without roles, and
 * any other path that merely ends in {@code /health} or {@code /healthy} does not inherit the
 * bypass. All of them run as a non-localhost caller, since access from localhost is granted
 * ADMIN before the health check is ever consulted.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessControlValveHealthCheckTests {
    private static final String REMOTE_HOST = "198.51.100.17";

    @Mock
    private IAccessControlService accessControls;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private AccessControlValve valve;

    @BeforeEach
    void setUp() {
        valve = new AccessControlValve(accessControls);

        CertificatePrincipal principal = new CertificatePrincipal();
        principal.setName("CN=probe.example.org");
        RequestContext.setPrincipal(principal);

        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteHost()).thenReturn(REMOTE_HOST);
        // A caller holding none of the roles the endpoint declares. This is what an ALB or
        // container probe looks like, and it is the only case in which the bypass is consulted.
        when(accessControls.checkAccess(anyString(), anyString(), anyString())).thenReturn(Boolean.FALSE);
        when(accessControls.isUserInRole(anyString(), anyString())).thenReturn(false);
        when(accessControls.isUserDenied(anyString())).thenReturn(false);
    }

    @AfterEach
    void clearContext() {
        RequestContext.clear();
    }

    private boolean accessAllowedFor(String path) {
        when(request.getRequestURI()).thenReturn(path);
        return valve.accessAllowed(request, response);
    }

    @Test
    void theAlbProbePathIsAllowedWithoutRoles() {
        assertTrue(accessAllowedFor("/rest/health"));
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void theContainerProbePathIsAllowedWithoutRoles() {
        assertTrue(accessAllowedFor("/rest/healthy"));
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * The reason the match is exact. Under the old suffix match, any endpoint whose path happened
     * to end in {@code /health} picked up the unauthenticated bypass -- including ones added later
     * under an unrelated controller, which is more than the probes need.
     */
    @Test
    void anotherControllersPathEndingInHealthIsDenied() {
        assertFalse(accessAllowedFor("/rest/destinations/health"));
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void anotherControllersPathEndingInHealthyIsDenied() {
        assertFalse(accessAllowedFor("/rest/destinations/healthy"));
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /** A trailing slash does not reach the controller mapping either, so it must not bypass. */
    @Test
    void aTrailingSlashOnTheProbePathIsDenied() {
        assertFalse(accessAllowedFor("/rest/health/"));
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void matrixParametersOnTheProbePathAreDenied() {
        assertFalse(accessAllowedFor("/rest/health;probe=true"));
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * The Hub mounts its controllers under {@code /rest}; the bare paths belong to the
     * Transformation Service, which installs its own valve and reaches none of this code.
     */
    @Test
    void theBareHealthPathIsDenied() {
        assertFalse(accessAllowedFor("/health"));
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
