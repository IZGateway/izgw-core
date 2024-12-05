package gov.cdc.izgateway.security.principal;

import java.util.Set;

public interface ScopeToRoleMapper {
    Set<String> mapScopesToRoles(Set<String> scopes);
}
