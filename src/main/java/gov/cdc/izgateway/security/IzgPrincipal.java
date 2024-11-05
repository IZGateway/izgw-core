package gov.cdc.izgateway.security;

import lombok.Data;

import java.util.Date;
import java.util.Set;

@Data
public abstract class IzgPrincipal implements java.security.Principal {
    String name;
    String organization;
    Date validFrom;
    Date validTo;
    String serialNumber;
    String issuer;
    String audience; // TODO: PCahill audience - aud claim in JWT - should indicate it's IZG Hub or Xform,
    // access control valve - is this audience us, is the issuer someone that we trust - so we can trust the JWT


    // roles v.s scopes - claims for scopes exist; groups or roles in JWT
    // we want to think about scopes - fine-grained
    // roles - coarse-grained - users has one or more roles.
    // scopes - *** map scopes to roles *** we should use this.
    Set<String> scopes;
    Set<String> roles;

    public abstract String getSerialNumberHex();
}
