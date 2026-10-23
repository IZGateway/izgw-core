package gov.cdc.izgateway.logging.event;

import java.util.Date;

/**
 * What every health report answers, regardless of how much detail the caller is entitled to.
 * <p>
 * Endpoints that vary their detail by caller return this type, so a caller can always read the
 * liveness answer without first establishing which view it received.
 *
 * @see Health
 * @see PublicHealth
 * @author Audacious Inquiry
 */
public interface IHealth {
	/**
	 * @return true if the server is healthy
	 */
	boolean isHealthy();

	/**
	 * @return the time the report was taken
	 */
	Date getStatusAt();
}
