package gov.cdc.izgateway.security;

import lombok.Data;

import java.util.Date;

@Data
public abstract class IzgPrincipal implements java.security.Principal {
    String name;
    String organization;
    Date validFrom;
    Date validTo;
    String serialNumber;
    // look for org claim
    // issuer - signer or issuer in cert; or iss claim in JWT
    // audience - aud claim in JWT - should indicate it's IZG Hub or Xform,
    // access control valve - is this audience us, is the issuer someone that we trust - so we can trust the JWT
    // roles v.s scopes - claims for scopes exist; groups or roles in JWT
    // scopes - in the level of CRUD
    // we want to think about scopes - fine-grained
    // roles - coarse-grained - users has one or more roles.
    // scopes - *** map scopes to roles *** we should use this.
    //

    public abstract String getSerialNumberHex();
}
