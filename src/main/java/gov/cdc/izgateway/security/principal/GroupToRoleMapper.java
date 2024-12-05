package gov.cdc.izgateway.security.principal;

import java.util.Set;

public interface GroupToRoleMapper {
    Set<String> mapGroupsToRoles(Set<String> groups);
}
