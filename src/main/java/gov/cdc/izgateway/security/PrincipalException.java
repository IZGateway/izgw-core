package gov.cdc.izgateway.security;

public class PrincipalException extends Exception {
    public PrincipalException(Throwable t) {
        super(t);
    }

    public PrincipalException(String message) {
        super(message);
    }
}
