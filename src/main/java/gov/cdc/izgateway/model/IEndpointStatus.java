package gov.cdc.izgateway.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import gov.cdc.izgateway.common.Constants;

/**
 * Represents the status of an endpoint in IZ Gateway, including status details, diagnostics, retry strategy, and audit information.
 * <p>
 * Provides methods to get and set status details, diagnostics, retry strategy, timestamps, and related endpoint information.
 * </p>
 *  
 * @author Audacious Inquiry
 *
 */
public interface IEndpointStatus extends IEndpoint {

	/**
	 * Gets the detail associated with the status.
	 * @return the status detail
	 */
	String getDetail();

	/**
	 * Gets the diagnostics associated with the status.
	 * @return the diagnostics
	 */
	String getDiagnostics();

	/**
	 * Gets the retry strategy associated with the status.
	 * @return the retry strategy
	 */
	String getRetryStrategy();

	/**
	 * Gets the status value.
	 * @return the status
	 */
	String getStatus();

	/**
	 * Gets the datetime the status was set.
	 * @return the status timestamp
	 */
	Date getStatusAt();

	/**
	 * Gets the host that set the status.
	 * @return the status host
	 */
	String getStatusBy();

	/**
	 * Gets the ID of the status entry.
	 * @return the status ID
	 */
	int getStatusId();

	/**
	 * Sets the destination ID associated with the status.
	 * @param destId the destination ID
	 */
	void setDestId(String destId);
	
	/**
	 * Gets the destination type ID.
	 * @return the destination type ID
	 */
	int getDestTypeId();

	/**
	 * Sets the URI value associated with the endpoint.
	 * @param destUri the destination URI
	 */
	void setDestUri(String destUri);

	/**
	 * Sets the protocol and version to use with the destination.
	 * @param destVersion the destination version
	 */
	void setDestVersion(String destVersion);

	/**
	 * Sets the detail associated with the status.
	 * @param detail the status detail
	 */
	void setDetail(String detail);

	/**
	 * Sets the diagnostic message associated with the status.
	 * @param diagnostics the diagnostic message
	 */
	void setDiagnostics(String diagnostics);

	/**
	 * Sets the jurisdiction identifier associated with the endpoint.
	 * @param jurisdictionId the jurisdiction ID
	 */
	@JsonIgnore
	void setJurisdictionId(int jurisdictionId);

	/**
	 * Sets the retry strategy associated with this status.
	 * @param retryStrategy the retry strategy
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