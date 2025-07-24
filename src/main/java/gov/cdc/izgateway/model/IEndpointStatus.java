package gov.cdc.izgateway.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import gov.cdc.izgateway.common.Constants;

/**
 * This interface represents the status of an endpoint in use by an IZ Gateway service.
 * It keeps track of the current status, the reason for that status, what system and when
 * the status was set, the retry strategy that should be applied to a message, and the diagnostics
 * associated with the failure.
 *  
 * @author Audacious Inquiry
 *
 */
public interface IEndpointStatus extends IEndpoint {

	/**
	 * @return the detail associated with the status.
	 */
	String getDetail();

	/**
	 * @return the diagnostics associated with the status.
	 */
	String getDiagnostics();

	/**
	 * @return the retry strategy associated with the status.
	 */
	String getRetryStrategy();

	/**
	 * @return	The status
	 */
	String getStatus();

	/**
	 * @return The datetime the status was set.
	 */
	Date getStatusAt();

	/**
	 * @return The host that set the status.
	 */
	String getStatusBy();

	/**
	 * @return The id of the status entry.
	 */
	int getStatusId();

	/**
	 * @param destId the destination Id associated with the status.
	 */
	void setDestId(String destId);
	
	int getDestTypeId();

	/**
	 * @param destUri the URI value associated with the endpoint to set.
	 */
	void setDestUri(String destUri);

	/**
	 * @param destVersion The protocol and version to use with the destination.
	 */
	void setDestVersion(String destVersion);

	/**
	 * Set the detail associated with the status
	 * @param detail the detail associated with the status
	 */
	void setDetail(String detail);

	/**
	 * Set the diagnostic message associated with the status
	 * @param diagnostics the diagnostic message
	 */
	void setDiagnostics(String diagnostics);

	@JsonIgnore
	/**
	 * Set the jurisdiction identifier associated with the endpoint
	 * @param jurisdictionId The jurisdiction identifier 
	 */
	void setJurisdictionId(int jurisdictionId);

	/**
	 * Set the Retry Strategy associated with this status.
	 * @param retryStrategy the retry Strategy 
	 */
	void setRetryStrategy(String retryStrategy);

	/**
	 * Set the datetime the status was set.
	 * @param statusAt The datetime the status was set.
	 */
	@JsonFormat(shape=Shape.STRING, pattern=Constants.TIMESTAMP_FORMAT) 
	void setStatusAt(Date statusAt);

	/**
	 * Set the host that set the status.
	 * @param statusBy The name of the host.
	 */
	void setStatusBy(String statusBy);

	/**
	 * Set the id of this status entry.
	 * @param statusId the id of this status entry
	 */
	@JsonIgnore
	void setStatusId(int statusId);

	/**
	 * Status value used when the endpoint is connected and responding appropriately with messages
	 */
	String CONNECTED = "Connected";
	/**
	 * Status value used when the endpoint failed to respond appropriately
	 */
	String CIRCUIT_BREAKER_THROWN = "Circuit Breaker Thrown";
	/**
	 * Status value used when the endpoint is under maintenance
	 */
	String UNDER_MAINTENANCE = "Under Maintenance";
	/**
	 * Status value used when status of the endpoint is unkown
	 */
	String UNKNOWN = "Unknown";

	/**
	 * @return Make a copy of this status entry
	 */
	IEndpointStatus copy();

	/**
	 * Set status and provenance for it.
	 * @param status	The status value to set.
	 */
	void setStatus(String status);

	/**
	 * Return true if the destination is connected.
	 * @return true if the destination is connected.
	 */
	boolean isConnected();

	/**
	 * @return true if the circuit breaker is thrown.
	 */
	boolean isCircuitBreakerThrown();

	/**
	 * Return true if, according to the current status, a new
	 * connection should be attempted.  A recent failure due to
	 * an invalid inbound message should NOT disable an outbound
	 * endpoint.
	 * 
	 * @return true if the destination is connected.
	 */
	boolean isAvailable();

	/**
	 * Set the type of the destination.
	 * @param destType	The type of destination.
	 */
	void setDestTypeId(int destType);

	String connected();

}