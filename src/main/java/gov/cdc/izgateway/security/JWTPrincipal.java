package gov.cdc.izgateway.security;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class JWTPrincipal extends IzgPrincipal {

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
}
