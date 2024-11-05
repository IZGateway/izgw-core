package gov.cdc.izgateway.security;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;

public class UnauthenticatedPrincipal extends IzgPrincipal {

    public UnauthenticatedPrincipal() {
        this.name = "Unauthenticated";
        this.organization = "Public";
        this.validFrom = new Date();
        this.validTo = new Date(Long.MAX_VALUE);
        this.serialNumber = "0";
        this.issuer = "None";
        this.audience = "Public";
        this.scopes = new HashSet<>();
        this.roles = new HashSet<>();
    }

    @Override
    public String getSerialNumberHex() {
        return String.format("%040x", new BigInteger(1, serialNumber.getBytes(StandardCharsets.UTF_8)));
    }
}