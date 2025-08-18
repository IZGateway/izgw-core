package gov.cdc.izgateway.security.crypto;

/**
 * Exception thrown when crypto operations fail.
 */
public class CryptoException extends Exception {
    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}