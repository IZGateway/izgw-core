package gov.cdc.izgateway.security;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import gov.cdc.izgateway.logging.RequestContext;
import gov.cdc.izgateway.logging.info.HostInfo;
import gov.cdc.izgateway.service.IAccessControlService;
import gov.cdc.izgateway.utils.SystemUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Performs access control checks on the requester based on certificate and
 * sourceIP address of the request.
 */
@Slf4j
@Component("valveAccessControl")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AccessControlValve extends ValveBase {
	/**
	 * Set this value to false to disable blacklist security.  This is here so we can turn it off by
	 * setting an environment variable if it causes any problems.
	 */
	@Value("${security.blacklist.disabled:false}")
	private boolean blacklistingDisabled;

	private static boolean NON_LOCAL_TESTING = false; // NOSONAR for debugging
    private static final List<String> LOCAL_HOST_IPS = Arrays.asList(HostInfo.LOCALHOST_IP4, "0:0:0:0:0:0:0:1", HostInfo.LOCALHOST_IP6);
    
    private final IAccessControlService accessControls;

    /**
     * Create the valve using the supplied access control service.
     * @param accessControls
     */
    @Autowired
	public AccessControlValve(IAccessControlService accessControls) {
    	this.accessControls = accessControls;
    }

    @Override
    public void invoke(Request req, Response resp) throws IOException, ServletException {
        if (accessAllowed(req, resp)) {
        	try {
        		this.getNext().invoke(req, resp);
        	} finally {
            	MDC.remove(Roles.NOT_ADMIN_HEADER);
            	RequestContext.getRoles().clear();
        	}
        }
    }
    
    /**
     * Implements main code using HttpServletRequest and HttpServletResponse to
     * enable testing with mocks.
     *
     * @param req  The request object
     * @param resp The response object
     * @return true if access is allowed
     */
    public boolean accessAllowed(HttpServletRequest req, HttpServletResponse resp) {
        IzgPrincipal principal = RequestContext.getPrincipal();

        String user = null;
        String path = req.getRequestURI();
        String host = req.getRemoteHost();
        boolean notAdminHeader = false;
        
        Set<String> theRoles = RequestContext.initRoles();
        // Save non-admin header status in the MDC [for testing, avoids admin overrides].
        if (req.getHeader(Roles.NOT_ADMIN_HEADER) != null) {
        	MDC.put(Roles.NOT_ADMIN_HEADER, "true");
        	theRoles.add(Roles.NOT_ADMIN_HEADER);
        	notAdminHeader = true;
        }
        
        if (principal == null) {
            if (!isLocalHost(req.getRemoteHost())) {
            	log.error("Access denied to protected URL {} address by unauthenticated user at {}", path, host);
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            user = "localhost@" + SystemUtils.getHostname();
        } else {
            user = principal.getName();
        }

        // Local user has Admin level access to everything.
        if (!notAdminHeader && isLocalHost(req.getRemoteHost())) {
        	theRoles.add(Roles.ADMIN);
    		// Access via localhost implies role = ADMIN.
    		log.trace("Access granted to protected URL {} address by {} at {}", path, user, host);
        	return true;
        }
        
        Boolean check = accessControls.checkAccess(user, req.getMethod(), path); 
        // True response means OK to access.
        if (Boolean.TRUE.equals(check)) {
        	log.trace("Access granted to protected URL {} address by {} at {}", path, user, host);
        	updateRoles(user, theRoles);
        	return true;
        }
        
        // False response means NOT OK to access.
        if (Boolean.FALSE.equals(check)) {  // NOSONAR Null is still possible here, SONAR flags it as always true
	        log.error("Access denied to protected URL {} address by {} at {}", path, user, host);
	        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	        return false;
        } 
        
        if (isSwagger(path, user)) {
        	log.trace("Access granted to swagger documentation {} address by {} at {}", path, user, host);
        	updateRoles(user, theRoles);
        	return true;
        } 
        
        // Null response means path unknown. This could be swagger documentation
        log.error("Access denied to unknown path {} address by {} at {}", path, user, host);
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return false;
    }

	private void updateRoles(String user, Set<String> theRoles) {
		if (accessControls.isUserInRole(user, Roles.ADMIN)) {
			theRoles.add(Roles.ADMIN);
		}
		if (accessControls.isUserBlacklisted(user)) {
			theRoles.add(Roles.BLACKLIST);
		}
	}

    private boolean isSwagger(String path, String user) {
		return path != null && path.startsWith("/swagger/") && accessControls.isUserInRole(user, Roles.ADMIN);
	}

	/**
	 * Return true if the host address is one of the IP addresses that identify it as running on localhost
	 * @param remoteHost	The host address to check.
	 * @return true if the remoteHost is localhost.
	 */
	public static boolean isLocalHost(String remoteHost) {
    	if (NON_LOCAL_TESTING) {
    		return false;
    	}
    	return LOCAL_HOST_IPS.contains(remoteHost);
    }
}
