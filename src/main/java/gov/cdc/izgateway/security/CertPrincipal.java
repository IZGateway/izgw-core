package gov.cdc.izgateway.security;

import lombok.Data;

import java.util.List;

@Data
public class CertPrincipal extends Principal {
    private String name;
    private List<String> roles;
}
