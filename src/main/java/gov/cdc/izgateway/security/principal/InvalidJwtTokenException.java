package gov.cdc.izgateway.security.principal;

public class InvalidJwtTokenException extends RuntimeException {
    public InvalidJwtTokenException(String message) {
        super(message);
    }
}
