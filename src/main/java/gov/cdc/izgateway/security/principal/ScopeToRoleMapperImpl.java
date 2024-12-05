package gov.cdc.izgateway.security.principal;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.TreeSet;

@Component
public class ScopeToRoleMapperImpl implements ScopeToRoleMapper {
    public Set<String> mapScopesToRoles(Set<String> scopes) {
        // Until we've defined a mapping between scopes and roles, we'll just return the scopes as roles
        return new TreeSet<>(scopes);
    }
}
