package gov.cdc.izgateway.security;

import lombok.Data;

import java.math.BigInteger;

@Data
public class CertificatePrincipal extends IzgPrincipal {

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
