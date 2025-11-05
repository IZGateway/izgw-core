package gov.cdc.izgateway.model;

import java.util.Date;

import gov.cdc.izgateway.security.IzgPrincipal;

/**
 * This interface is used to mark entities which have audit fields for createdBy, createdOn, updatedBy, and updatedOn.
 * @author Audacious Inquiry
 *
 */
public interface DbAudit {
	/** Get the user who created the record
	 *  This will be in username@hostname format, where hostname is the system from which the change was made, and username is the 
	 *  principal making the change.
	 *  When changes are made by automated systems, the username portion should identify the system or process making the change,
	 *  and will typically be the external DNS name associated with the service, while hostname will be the internal host name 
	 *  (e.g., specific ECS hostname). 
	 * @return The user who created the record
	 */
	String getCreatedBy();
	/** Set the user who created the record 
	 * @param createdBy The user who created the record
	 */
	void setCreatedBy(String createdBy);
	/** 
	 * Get the user who updated the record 
	 * @return The user who created the record
	 */
	String getUpdatedBy();
	/** 
	 * Set the user who updated the record
	 * @param updatedBy The user who updated the record 
	 */
	void setUpdatedBy(String updatedBy);
	/**
	 * Get the date/time when the record was created
	 * @return The date/time when the record was created
	 */
	Date getCreatedOn();
	/**
	 * Set the date/time when the record was created
	 * @param createdAt The date/time when the record was created
	 */
	void setCreatedOn(Date createdAt);
	/**
	 * Get the date/time when the record was last updated
	 * @return The date/time when the record was last updated
	 */
	Date getUpdatedOn();
	/**
	 * Set the date/time when the record was last updated
	 * @param updatedAt The date/time when the record was last updated
	 */
	void setUpdatedOn(Date updatedAt);
	
	/**
	 * Set the created fields to the current date and server principal name and host.
	 */
	void setCreated();
	
	/**
	 * Set the created fields to the current date and user principal name and host.
	 * @param principal The user principal
	 * @param hostname The host name
	 */
	void setCreated(IzgPrincipal principal, String hostname);
	
	/**
	 * Set the updated fields to the current date and server principal name and host.
	 */
	void setUpdated();
	
	/**
	 * Set the updated fields to the current date and user principal name and host.
	 * @param principal
	 * @param hostname
	 */
	void setUpdated(IzgPrincipal principal, String hostname);
}
