package gov.cdc.izgateway.security.principal;

import java.util.Set;
import java.util.TreeSet;

public class RoleMapper {
    public static Set<String> mapScopesToRoles(Set<String> scopes) {
        // Until we've defined a mapping between scopes and roles, we'll just return the scopes as roles
        return new TreeSet<>(scopes);
    }

}
