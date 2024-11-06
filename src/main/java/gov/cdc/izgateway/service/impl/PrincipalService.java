package gov.cdc.izgateway.service.impl;

import com.nimbusds.jose.jwk.source.DefaultJWKSetCache;
import com.nimbusds.jose.jwk.source.JWKSetCache;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import gov.cdc.izgateway.security.CertPrincipal;
import gov.cdc.izgateway.security.IzgPrincipal;
import gov.cdc.izgateway.security.JWTPrincipal;
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
import java.util.Objects;

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

    private IzgPrincipal createJWTPrincipal(HttpServletRequest request) {

        if (StringUtils.isBlank(jwkSetUri)) {
            log.warn("No JWT set URI configured.  JWT authentication is disabled.");
            return null;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No JWT token found in Authorization header");
            return null;
        }

        JwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        Jwt jwt;

        try {
            String token = authHeader.substring(7);
            jwt = jwtDecoder.decode(token);
        } catch (Exception e) {
            log.warn("Error parsing JWT token", e);
            return null;
        }

        IzgPrincipal principal = new JWTPrincipal();
        principal.setName(jwt.getSubject());
        principal.setOrganization(jwt.getClaim("organization"));
        principal.setValidFrom(Date.from(Objects.requireNonNull(jwt.getIssuedAt())));
        principal.setValidTo(Date.from(Objects.requireNonNull(jwt.getExpiresAt())));
        principal.setSerialNumber(jwt.getId());
        principal.setIssuer(jwt.getIssuer().toString());
        principal.setAudience(jwt.getAudience());

        return principal;
    }

    private IzgPrincipal createCertPrincipal(HttpServletRequest req) {
        IzgPrincipal principal = new CertPrincipal();
        X509Certificate[] certs = (X509Certificate[]) req.getAttribute(Globals.CERTIFICATES_ATTR);

        if (certs == null || certs.length == 0) {
            log.warn("No certificates found in request.");
            return null;
        }

        X509Certificate cert = certs[0];
        X500Principal subject = cert.getSubjectX500Principal();

        Map<String, String> parts = X500Utils.getParts(subject);
        principal.setName(parts.get(X500Utils.COMMON_NAME));
        String o = parts.get(X500Utils.ORGANIZATION);
        if (StringUtils.isBlank(o)) {
            o = parts.get(X500Utils.ORGANIZATION_UNIT);
        }
        principal.setOrganization(o);

        principal.setValidFrom(cert.getNotBefore());
        principal.setValidTo(cert.getNotAfter());
        principal.setSerialNumber(String.valueOf(cert.getSerialNumber()));
        principal.setIssuer(cert.getIssuerX500Principal().getName());

        return principal;
    }
}