package gov.cdc.izgateway.security.service;

import gov.cdc.izgateway.security.IzgPrincipal;
import jakarta.servlet.http.HttpServletRequest;

public interface PrincipalService {
    IzgPrincipal getPrincipal(HttpServletRequest request);
}
