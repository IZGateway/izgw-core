package gov.cdc.izgateway.security;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigInteger;

/**
 * A class representing a principal based on an X.509 certificate
 * @author Audacious Inquiry
 */
@Data
@EqualsAndHashCode(callSuper=true)
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
