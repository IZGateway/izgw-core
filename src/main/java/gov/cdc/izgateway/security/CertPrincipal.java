package gov.cdc.izgateway.security;

import lombok.Data;

import java.math.BigInteger;
import java.util.List;

@Data
public class CertPrincipal extends IzgPrincipal {
    private List<String> roles;

    public String getSerialNumberHex() {
        // If isNumeric, return the hex representation
        if (serialNumber.matches("\\d+")) {
            return new BigInteger(serialNumber).toString(16).toUpperCase();
        }
        else {
            return null;
        }
    }
}
