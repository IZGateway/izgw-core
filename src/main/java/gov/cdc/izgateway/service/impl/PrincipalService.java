package gov.cdc.izgateway.service.impl;

import gov.cdc.izgateway.security.CertPrincipal;
import gov.cdc.izgateway.security.IzgPrincipal;
import gov.cdc.izgateway.security.PrincipalException;
import gov.cdc.izgateway.service.IPrincipalService;
import gov.cdc.izgateway.utils.X500Utils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Globals;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Map;

@Service
@Slf4j
public class PrincipalService implements IPrincipalService {

    @Override
    public IzgPrincipal getPrincipal(HttpServletRequest request) throws PrincipalException {
        // java.security.Principal p = request.getUserPrincipal();
        // override - isUSerInRole,

        IzgPrincipal izgPrincipal = null;

        if (request != null) {
            if (request.getAttribute("jakarta.servlet.request.X509Certificate") != null) {
                izgPrincipal = createCertPrincipal(request);
            } else {
                izgPrincipal = createJWTPrincipal(request);
            }
            // TODO: reverse this, as we want to use the Bearer token first.
            // Also, if JWT fails, fall through to the cert.
        }

        return izgPrincipal;
    }

    private IzgPrincipal createCertPrincipal(HttpServletRequest req) throws PrincipalException{
        IzgPrincipal izgPrincipal = new CertPrincipal();

        X509Certificate[] certs = (X509Certificate[]) req.getAttribute(Globals.CERTIFICATES_ATTR);

        if (certs == null || certs.length == 0) {
            log.error("No certificates found in request.");
            throw new PrincipalException("No certificates found in request.");
        }

        X509Certificate cert = certs[0];

        X500Principal subject = cert.getSubjectX500Principal();
        Map<String, String> parts = X500Utils.getParts(subject);

        izgPrincipal.setName(parts.get(X500Utils.COMMON_NAME));

        // Get organization, and if not present, look for the Organizational Unit
        String o = parts.get(X500Utils.ORGANIZATION);
        if (StringUtils.isBlank(o)) {
            o = parts.get(X500Utils.ORGANIZATION_UNIT);
        }
        izgPrincipal.setOrganization(o);
        izgPrincipal.setValidFrom(cert.getNotBefore());
        izgPrincipal.setValidTo(cert.getNotAfter());
        izgPrincipal.setSerialNumber(String.valueOf(cert.getSerialNumber()));

        return izgPrincipal;
    }

    // TODO - list of
    // OKTA
    // APHL - OKTA
    // Self-hosted - someone else's - config
    private IzgPrincipal createJWTPrincipal(HttpServletRequest request) throws PrincipalException {
        String authHeader = request.getHeader("Authorization");

        if ( authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("No JWT token found in Authorization header");
            return new CertPrincipal();
        }

        JwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri("https://dev-21905037.okta.com/oauth2/auskk2stilvj7Jp7R5d7/v1/keys").build();
        Jwt jwt;

        try {
            String token = authHeader.substring(7);

            log.info("*** TRYING JWT");
            jwt = jwtDecoder.decode(token);
            log.info("*** JWT claims for current request: {}", jwt.getClaims());

        } catch (Exception e) {
            log.error("Error parsing JWT token", e);
            throw new PrincipalException(e);
        }

        IzgPrincipal izgPrincipal = new CertPrincipal();

        izgPrincipal.setName(jwt.getSubject());
        izgPrincipal.setOrganization("JWT PRINCIPAL ORG NOT IMPLEMENTED");
        izgPrincipal.setValidFrom(Date.from(jwt.getIssuedAt()));
        izgPrincipal.setValidTo(Date.from(jwt.getExpiresAt()));
        izgPrincipal.setSerialNumber(jwt.getId());

        return izgPrincipal;
    }
}
