package gov.cdc.izgateway.service.impl;

import gov.cdc.izgateway.security.CertPrincipal;
import gov.cdc.izgateway.security.JWTPrincipal;
import gov.cdc.izgateway.security.Principal;
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
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Map;

@Service
@Slf4j
public class PrincipalService implements IPrincipalService {

    @Override
    public Principal getPrincipal(HttpServletRequest request) {
        Principal principal = null;

        if (request != null) {
            if (request.getAttribute("jakarta.servlet.request.X509Certificate") != null) {
                principal = createCertPrincipal(request);
            } else {
                principal = createJWTPrincipal(request);
            }
        }

        return principal;
    }

    private Principal createCertPrincipal(HttpServletRequest req) {
        Principal principal = new CertPrincipal();

        X509Certificate[] certs = (X509Certificate[]) req.getAttribute(Globals.CERTIFICATES_ATTR);

        // TODO - check if null or no cert
        X509Certificate cert = certs[0];

        X500Principal subject = cert.getSubjectX500Principal();
        Map<String, String> parts = X500Utils.getParts(subject);

        principal.setName(parts.get(X500Utils.COMMON_NAME));

        // Get organization, and if not present, look for the Organizational Unit
        String o = parts.get(X500Utils.ORGANIZATION);
        if (StringUtils.isBlank(o)) {
            o = parts.get(X500Utils.ORGANIZATION_UNIT);
        }
        principal.setOrganization(o);
        principal.setValidFrom(cert.getNotBefore());
        principal.setValidTo(cert.getNotAfter());
        principal.setUniqueId(String.valueOf(cert.getSerialNumber()));

        return principal;
    }

    private Principal createJWTPrincipal(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if ( authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("No JWT token found in Authorization header");
            return new CertPrincipal();
        }

        JwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri("https://dev-21905037.okta.com/oauth2/auskk2stilvj7Jp7R5d7/v1/keys").build();

//        JwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri("https://dev-21905037.okta.com/oauth2/auskk2stilvj7Jp7R5d7/v1/keys").jwtProcessorCustomizer(jwtProcessor -> {
//            jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType("application/okta-internal-at+jwt")));
//        }).build();

        try {
            String token = authHeader.substring(7);

            log.info("*** TRYING JWT");
            Jwt jwt = jwtDecoder.decode(token);
            log.info("*** JWT claims for current request: {}", jwt.getClaims());

        } catch (Exception e) {
            log.error("Error parsing JWT token", e);
        }

        Principal principal = new CertPrincipal();

        principal.setName("JWT PRINCIPAL NOT IMPLEMENTED");
        principal.setOrganization("JWT PRINCIPAL ORG NOT IMPLEMENTED");
        principal.setValidFrom(new Date());
        principal.setValidTo(new Date());
        principal.setUniqueId("JWT PRINCIPAL ID NOT IMPLEMENTED");

        return principal;
    }
}
