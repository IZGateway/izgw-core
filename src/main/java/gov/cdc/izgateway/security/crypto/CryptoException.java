package gov.cdc.izgateway.security.crypto;

/**
 * Exception thrown when crypto operations fail.
 */
public class CryptoException extends Exception {
    /** Default serial version ID */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor with message
	 * @param message The message for the exception
	 */
	public CryptoException(String message) {
        super(message);
    }

	/**
	 * Constructor with message and cause
	 * @param message	The message for the exception
	 * @param cause	The cause of the exception
	 */
    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}