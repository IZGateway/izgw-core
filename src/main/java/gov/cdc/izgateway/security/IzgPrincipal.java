package gov.cdc.izgateway.security;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
public abstract class IzgPrincipal implements java.security.Principal {
    String name;
    String organization;
    Date validFrom;
    Date validTo;
    String serialNumber;
    String issuer;
    List<String> audience;
    Set<String> scopes;
    Set<String> roles;

    public abstract String getSerialNumberHex();
}
