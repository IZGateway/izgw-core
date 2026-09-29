package gov.cdc.izgateway.common;

/**
 * A minimal replacement for the retired {@code javax.xml.ws.http.HTTPException}, carrying just
 * an HTTP status code. Used where callers need to throw/catch an exception representing an
 * arbitrary HTTP response status without depending on the JAX-WS API.
 *
 * @author Audacious Inquiry
 *
 */
@SuppressWarnings("serial")
public class HttpStatusException extends RuntimeException {
	private final int statusCode;

	/**
	 * Create an HttpStatusException for the given HTTP status code.
	 * @param statusCode	The HTTP status code.
	 */
	public HttpStatusException(int statusCode) {
		super("HTTP Status: " + statusCode);
		this.statusCode = statusCode;
	}

	/**
	 * Get the HTTP status code associated with this exception.
	 * @return	The HTTP status code.
	 */
	public int getStatusCode() {
		return statusCode;
	}
}
