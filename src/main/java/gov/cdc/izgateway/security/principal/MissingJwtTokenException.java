package gov.cdc.izgateway.security.principal;

public class MissingJwtTokenException extends RuntimeException {
    public MissingJwtTokenException(String message) {
        super(message);
    }
}
