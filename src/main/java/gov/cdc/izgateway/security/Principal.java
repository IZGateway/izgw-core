package gov.cdc.izgateway.security;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public abstract class Principal {
    String name; // Used to be commonName
    String organization;
    Date validFrom;
    Date validTo;
    String uniqueId; // Used to be serialNumber
}
