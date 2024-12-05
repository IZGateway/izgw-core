package gov.cdc.izgateway.security.principal;

import gov.cdc.izgateway.principal.provider.JwtPrincipalProvider;
import gov.cdc.izgateway.security.IzgPrincipal;
import gov.cdc.izgateway.security.JWTPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Slf4j
@Component
public class JwtSharedSecretPrincipalProvider implements JwtPrincipalProvider {
    @Value("${jwt.shared-secret:}")
    private String sharedSecret;

    private final GroupToRoleMapper groupToRoleMapper;
    private final ScopeToRoleMapper scopeToRoleMapper;

    @Autowired
    public JwtSharedSecretPrincipalProvider(@Nullable GroupToRoleMapper groupToRoleMapper,
                                            @Nullable ScopeToRoleMapper scopeToRoleMapper) {
        this.groupToRoleMapper = groupToRoleMapper;
        this.scopeToRoleMapper = scopeToRoleMapper;
    }

    @Override
    public IzgPrincipal createPrincipalFromJwt(HttpServletRequest request) {
        if (StringUtils.isBlank(sharedSecret)) {
            log.warn("No JWT shared secret was set. JWT authentication is disabled.");
            return null;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No JWT token found in Authorization header");
            return null;
        }

        Claims claims = parseJwt(authHeader);
        if (claims == null) {
            return null;
        }
        log.debug("JWT claims for current request: {}", claims);

        return buildPrincipal(claims);
    }

    private Claims parseJwt(String authHeader) {
        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(sharedSecret.getBytes());
            String token = authHeader.substring(7);
            return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
        } catch (Exception e) {
            log.warn("Error parsing JWT token", e);
            return null;
        }
    }

    private IzgPrincipal buildPrincipal(Claims claims) {
        IzgPrincipal principal = new JWTPrincipal();
        principal.setName(claims.getSubject());
        principal.setOrganization(claims.get("organization", String.class));
        principal.setValidFrom(claims.getNotBefore());
        principal.setValidTo(claims.getExpiration());
        principal.setSerialNumber(claims.getId());
        principal.setIssuer(claims.getIssuer());
        principal.setAudience(Collections.singletonList(claims.getAudience()));
        addRolesFromScopes(claims, principal);
        addRolesFromGroups(claims, principal);
        log.debug("Roles created from JWT: {}", principal.getRoles());

        return principal;
    }

    private void addRolesFromScopes(Claims claims, IzgPrincipal principal) {
        if (scopeToRoleMapper == null) {
            log.debug("No scope to role mapper was set. Skipping scope to role mapping.");
            return;
        }

        TreeSet<String> scopes = extractScopes(claims);
        principal.getRoles().addAll(scopeToRoleMapper.mapScopesToRoles(scopes));
    }

    private TreeSet<String> extractScopes(Claims claims) {
        TreeSet<String> scopes = new TreeSet<>();
        String scopeString = claims.get("scope", String.class);
        if (!StringUtils.isEmpty(scopeString)) {
            Collections.addAll(scopes, scopeString.split(" "));
        }
        return scopes;
    }

    private void addRolesFromGroups(Claims claims, IzgPrincipal principal) {
        if (groupToRoleMapper == null) {
            log.debug("No group to role mapper was set. Skipping group to role mapping.");
            return;
        }

        List<String> groupsList = claims.get("groups", List.class);
        if (groupsList == null || groupsList.isEmpty()) {
            return;
        }
        Set<String> groups = new TreeSet<>(groupsList);

        Set<String> roles = groupToRoleMapper.mapGroupsToRoles(groups);
        principal.getRoles().addAll(roles);
    }
}
