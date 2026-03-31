package gov.cdc.izgateway.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Indicates a bad request, such as a missing required field or an invalid value.
 * 
 * @author Audacious Inquiry
 *
 */
@SuppressWarnings("serial")
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {
	/**
	 * Create a BadRequestException with no message or cause.
	 */
	public BadRequestException() {
		super();
	}

	/**
	 * Create a BadRequestException with the given message.
	 * @param msg	The message to include in the exception
	 */
	public BadRequestException(String msg) {
		super(msg);
	}

	/**
	 * Create a BadRequestException with the given cause.
	 * @param msg	The message to include in the exception
	 * @param cause	The cause of the exception
	 */
	public BadRequestException(String msg, Throwable cause) {
		super(msg, cause);
	}
}