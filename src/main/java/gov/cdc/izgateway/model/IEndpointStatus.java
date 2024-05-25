package gov.cdc.izgateway.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import gov.cdc.izgateway.common.Constants;

public interface IEndpointStatus extends IEndpoint {

	String getDetail();

	String getDiagnostics();

	String getRetryStrategy();

	String getStatus();

	Date getStatusAt();

	String getStatusBy();

	int getStatusId();

	void setDestId(String destId);

	void setDestUri(String destUri);

	void setDestVersion(String destVersion);

	void setDetail(String detail);

	void setDiagnostics(String diagnostics);

	@JsonIgnore
	void setJurisdictionId(int jurisdictionId);

	void setRetryStrategy(String retryStrategy);

	@JsonFormat(shape=Shape.STRING, pattern=Constants.TIMESTAMP_FORMAT) 
	void setStatusAt(Date statusAt);

	void setStatusBy(String statusBy);

	@JsonIgnore
	void setStatusId(int statusId);

	String CONNECTED = "Connected";
	String CIRCUIT_BREAKER_THROWN = "Circuit Breaker Thrown";
	String UNDER_MAINTENANCE = "Under Maintenance";

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

	void setDestTypeId(int destType);

	String connected();

}