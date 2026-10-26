package gov.cdc.izgateway.logging.event;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import gov.cdc.izgateway.common.Constants;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The health status of the server as shown to a caller that has not authenticated.
 * <p>
 * This is a deliberate allowlist rather than a redacted copy of {@link Health}: it carries
 * only the liveness answer and the time it was taken. Everything else {@code Health} reports to
 * authenticated callers -- host and server names, environment, ingress and egress addresses, AWS
 * region, the database in use, the build identifier, the access control action, request and
 * success volumes, event count, start time, startup duration, the last healthy and unhealthy
 * timestamps, the reason for the last health change, and the message of the last exception --
 * has no field here and so cannot be disclosed, including when new fields are later added to
 * {@code Health}.
 * <p>
 * Note that the exception message reaches the wire through {@code Health}'s hand-written
 * {@link Health#getLastException()}, not through the {@code @JsonIgnore}d field behind it, so
 * inspecting {@code Health}'s fields alone understates what the full report carries. That is the
 * case this allowlist is built for.
 * <p>
 * Load balancer and container probes are unaffected: they match on the HTTP status code, which
 * is unchanged.
 *
 * @see Health#publicView()
 * @author Audacious Inquiry
 */
@JsonPropertyOrder({ "healthy", "statusAt" })
public class PublicHealth implements IHealth {
	@JsonProperty
	@Schema(description="True if the server is healthy")
	private final boolean healthy;

	@JsonProperty @JsonFormat(shape=Shape.STRING, pattern = Constants.TIMESTAMP_FORMAT)
	@Schema(description="Timestamp of the health report")
	private final Date statusAt;

	/**
	 * Construct the public view of a health report.
	 *
	 * @param healthy  true if the server is healthy
	 * @param statusAt the time the report was taken
	 */
	public PublicHealth(boolean healthy, Date statusAt) {
		this.healthy = healthy;
		this.statusAt = statusAt == null ? null : new Date(statusAt.getTime());
	}

	@Override
	public boolean isHealthy() {
		return healthy;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Returns a defensive copy, so a caller cannot reach back into this report through it.
	 */
	@Override
	public Date getStatusAt() {
		return statusAt == null ? null : new Date(statusAt.getTime());
	}
}
