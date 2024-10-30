package gov.cdc.izgateway.service;

import gov.cdc.izgateway.security.Principal;
import jakarta.servlet.http.HttpServletRequest;

public interface IPrincipalService {
    Principal getPrincipal(HttpServletRequest request);
}
