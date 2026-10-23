package gov.cdc.izgateway.security;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;

@Data
public abstract class IzgPrincipal implements java.security.Principal {
    String name;
    String organization;
    Date validFrom;
    Date validTo;
    String serialNumber;
    String issuer;
    List<String> audience = new ArrayList<>();
    Set<String> scopes = new TreeSet<>();
    Set<String> roles = new TreeSet<>();

    public abstract String getSerialNumberHex();

    /**
     * Whether this principal represents a caller that successfully authenticated.
     * <p>
     * Used to decide how much detail a response may carry. The default is {@code true}
     * so that every principal produced by a real authentication mechanism is treated as
     * authenticated; a principal type that stands in for an anonymous caller must
     * override this and return {@code false}.
     *
     * @return true if the caller authenticated
     * @see UnauthenticatedPrincipal
     */
    public boolean isAuthenticated() {
        return true;
    }
}
