package gov.cdc.izgateway.security;

import gov.cdc.izgateway.security.principal.GroupToRoleMapper;
import gov.cdc.izgateway.security.principal.ScopeToRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class JWTPrincipal extends IzgPrincipal {

    private final GroupToRoleMapper groupToRoleMapper;
    private final ScopeToRoleMapper scopeToRoleMapper;

    public JWTPrincipal(Jwt jwt,
                        String rolesClaim,
                        String scopesClaim,
                        @Nullable GroupToRoleMapper groupToRoleMapper,
                        @Nullable ScopeToRoleMapper scopeToRoleMapper) {
        this.groupToRoleMapper = groupToRoleMapper;
        this.scopeToRoleMapper = scopeToRoleMapper;

        setName(jwt.getSubject());
        setOrganization(getClaimNestedAsString(jwt, "organization"));
        setValidFrom(Date.from(Objects.requireNonNull(jwt.getIssuedAt())));
        setValidTo(Date.from(Objects.requireNonNull(jwt.getExpiresAt())));
        setSerialNumber(jwt.getId());
        // Nimbus doesn't like non-URI issuers, pull via raw claim
        setIssuer(getClaimNestedAsString(jwt, "iss"));
        setAudience(jwt.getAudience());
        addScopes(getClaimNested(jwt, scopesClaim));
        addRolesFromScopes(getClaimNested(jwt, scopesClaim));
        addRolesFromGroups(getClaimNested(jwt, rolesClaim));
    }

    public String getSerialNumberHex() {
        // If isNumeric, return the hex representation
        if (serialNumber.matches("\\d+")) {
            return new BigInteger(serialNumber).toString(16).toUpperCase();
        }

        // If isUUID, return the hex representation
        if (isUUID(serialNumber)) {
            return serialNumber.replace("-", "").toUpperCase();
        }

        // Return generic hex representation of the string
        return String.format("%040x", new BigInteger(1, serialNumber.getBytes(StandardCharsets.UTF_8)));

    }

    private boolean isUUID(String string) {
        try {
            UUID.fromString(string);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private void addScopes(Object scopesClaim) {
        TreeSet<String> scopes = extractClaimList(scopesClaim);
        setScopes(scopes);
    }

    private void addRolesFromScopes(Object scopesClaim) {
        if (scopeToRoleMapper == null) {
            log.debug("No scope to role mapper was set. Skipping scope to role mapping.");
            return;
        }
        TreeSet<String> scopes = extractClaimList(scopesClaim);
        getRoles()
                .addAll(
                        scopeToRoleMapper.mapScopesToRoles(scopes)
                );
    }

    private TreeSet<String> extractClaimList(Object scopeList) {
        // Oauth2 RFC (https://www.rfc-editor.org/rfc/rfc6749) states scopes
        // should be "expressed as a list of space-delimited, case-sensitive strings"
        // However, Okta seems to send back an array of strings.
        TreeSet<String> scopes = new TreeSet<>();
        if (scopeList instanceof String scopeString) {
            if (!StringUtils.isEmpty(scopeString)) {
                Collections.addAll(scopes, scopeString.split(" "));
            }
        } else if (scopeList instanceof Collection<?> collection) {
            collection.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .forEach(scopes::add);
        }
        return scopes;
    }

    private void addRolesFromGroups(Object groupsClaim) {
        if (groupToRoleMapper == null) {
            log.debug("No group to role mapper was set. Skipping group to role mapping.");
            return;
        }

        TreeSet<String> groupsList = extractClaimList(groupsClaim);
        if (groupsList.isEmpty()) {
            return;
        }

        getRoles()
                .addAll(
                        groupToRoleMapper.mapGroupsToRoles(groupsList)
                );
    }

    private String getClaimNestedAsString(Jwt jwt, String claimName) {
        Object claimValue = getClaimNested(jwt, claimName);
        return claimValue != null ? claimValue.toString() : null;
    }

    private Object getClaimNested(Jwt jwt, String claimPath) {
        String[] claimPathParts = claimPath.split("\\.");
        Object claim = jwt.getClaim(claimPathParts[0]);

        if (claimPathParts.length > 1 && !(claim instanceof Map<?,?>)) {
            /*
             Catches situation where you have configured groups.access.
             The initial pull of groups returns an array list.
             Without this, we'd end up returning that pull from groups
             as the claim Object, and ignore the fact that the user had
             configured another level.
             Seemed dangerous.
            */
            return null;
        }

        if (claim instanceof Map<?, ?> map && claimPathParts.length > 1) {
            for (int i = 1; i < claimPathParts.length; i++) {
                claim = map.get(claimPathParts[i]);
                if (claim instanceof Map<?, ?>) {
                    map = (Map<?, ?>) claim;
                } else if (i < claimPathParts.length - 1) {
                    /*
                    Again, catches a situation where the user has configured groups.level1.level2 and at level1
                    we have gotten a non-Map.
                    So basically level2 didn't exist, but we don't want to return
                    the value pulled from level1.
                     */
                    return null;
                }
            }
        }
        return claim;
    }
}
