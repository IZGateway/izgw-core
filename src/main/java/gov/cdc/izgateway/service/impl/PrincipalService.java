package gov.cdc.izgateway.service.impl;

import com.nimbusds.jose.jwk.source.DefaultJWKSetCache;
import com.nimbusds.jose.jwk.source.JWKSetCache;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import gov.cdc.izgateway.security.CertPrincipal;
import gov.cdc.izgateway.security.IzgPrincipal;
import gov.cdc.izgateway.security.JWTPrincipal;
import gov.cdc.izgateway.security.PrincipalException;
import gov.cdc.izgateway.security.UnauthenticatedPrincipal;
import gov.cdc.izgateway.service.IPrincipalService;
import gov.cdc.izgateway.utils.X500Utils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Globals;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.security.auth.x500.X500Principal;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

@Service
@Slf4j
public class PrincipalService implements IPrincipalService {
    @Value("${jwt.jwk-set-uri}")
    private String jwkSetUri;

    /**
     * Get the principal from the request. This will first try to get the principal from the JWT token,
     * and if that fails, it will try to get the principal from the certificate. If neither is found, it
     * will return an UnauthenticatedPrincipal.
     * @param request
     * @return
     * @throws PrincipalException
     */
    @Override
    public IzgPrincipal getPrincipal(HttpServletRequest request) {

        IzgPrincipal izgPrincipal = null;

        if (request != null) {
            izgPrincipal = createJWTPrincipal(request);
            if (izgPrincipal == null) {
                izgPrincipal = createCertPrincipal(request);
            }
        }

        if (izgPrincipal == null) {
            izgPrincipal = new UnauthenticatedPrincipal();
        }

        return izgPrincipal;
    }

    // TODO - list of
    // OKTA
    // APHL - OKTA
    // Self-hosted - someone else's - config
    private IzgPrincipal createJWTPrincipal(HttpServletRequest request) {

        // Return null if jwkSetUri is not set
        if (StringUtils.isBlank(jwkSetUri)) {
            log.warn("No JWT set URI configured.  JWT authentication is disabled.");
            return null;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("No JWT token found in Authorization header");
            return null;
        }

        JwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        Jwt jwt;

        try {
            String token = authHeader.substring(7);

            log.info("*** TRYING JWT");
            jwt = jwtDecoder.decode(token);
            log.info("*** JWT claims for current request: {}", jwt.getClaims());

        } catch (Exception e) {
            log.error("Error parsing JWT token", e);
            return null;
        }

        IzgPrincipal izgPrincipal = new JWTPrincipal();

        izgPrincipal.setName(jwt.getSubject());
        izgPrincipal.setOrganization("JWT PRINCIPAL ORG NOT IMPLEMENTED");
        izgPrincipal.setValidFrom(Date.from(jwt.getIssuedAt()));
        izgPrincipal.setValidTo(Date.from(jwt.getExpiresAt()));
        izgPrincipal.setSerialNumber(jwt.getId());

        return izgPrincipal;
    }

    private IzgPrincipal createCertPrincipal(HttpServletRequest req) {
        IzgPrincipal izgPrincipal = new CertPrincipal();

        X509Certificate[] certs = (X509Certificate[]) req.getAttribute(Globals.CERTIFICATES_ATTR);

        if (certs == null || certs.length == 0) {
            log.warn("No certificates found in request.");
            return null;
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

    /*
        TODO: PCahill - make sure these comments from the review are addressed:
        java.security.Principal p = request.getUserPrincipal();
        override - isUSerInRole,
     */

}