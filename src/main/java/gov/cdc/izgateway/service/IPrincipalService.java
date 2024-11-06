package gov.cdc.izgateway.service;

import gov.cdc.izgateway.security.IzgPrincipal;
import jakarta.servlet.http.HttpServletRequest;

public interface IPrincipalService {
    IzgPrincipal getPrincipal(HttpServletRequest request);
}
