package gov.cdc.izgateway.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import gov.cdc.izgateway.repository.DynamoDbRepository;
import gov.cdc.izgateway.utils.SystemUtils;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;

/**
 * Base class to provide audit fields for DynamoDB entities.
 * @author Audacious Inquiry
 */
@Data
public abstract class DynamoDbAudit implements DynamoDbEntity {
	Date createdOn;
	Date updatedOn;
	@JsonIgnore
	String createdBy;
	@JsonIgnore
	String updatedBy;
	
	/**
	 * Default constructor initializes created and updated fields to current date and server principal.
	 * These can be overridden later as needed, but ensures they are never null.
	 */
	public DynamoDbAudit() {
		setCreated();
		setUpdated();
	}
	/**
	 * Copy constructor
	 * @param other	the other audit object to copy from
	 */
	public DynamoDbAudit(DbAudit other) {
		createdOn = other.getCreatedOn();
		updatedOn = other.getUpdatedOn();
		createdBy = other.getCreatedBy();
		updatedBy = other.getUpdatedBy();
	} 
	
	@Override
	@JsonIgnore
	@DynamoDbConvertedBy(DateConverter.class)
	public Date getCreatedOn() {
		return createdOn;
	}
	
	@Override
	@JsonIgnore
	@DynamoDbConvertedBy(DateConverter.class)
	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}
	
	@Override
	@JsonIgnore
	@DynamoDbConvertedBy(DateConverter.class)
	public Date getUpdatedOn() {
		return updatedOn;
	}
	
	@Override
	@JsonIgnore
	@DynamoDbConvertedBy(DateConverter.class)
	public void setUpdatedOn(Date updatedOn) {
		this.updatedOn = updatedOn;
	}
	
	/**
	 * Set the created fields to the current date and server principal name and host.
	 */
	@Override
	public void setCreated() {
		this.createdOn = new Date();
		this.createdBy = String.format("%s@%s", DynamoDbRepository.getServerName(), SystemUtils.getHostname());
	}
	
	/**
	 * Set the created fields to the current date and user principal name and host.
	 * @param principal The user principal
	 * @param hostname The host name
	 */
	@Override
	public void setCreated(String principal, String hostname) {
		this.createdOn = new Date();
		this.createdBy = String.format("%s@%s", principal, hostname);
	}
	
	/**
	 * Set the updated fields to the current date and server principal name and host.
	 */
	@Override
	public void setUpdated() {
		this.updatedOn = new Date();
		this.updatedBy = String.format("%s@%s", DynamoDbRepository.getServerName(), SystemUtils.getHostname());
	}
	
	/**
	 * Set the updated fields to the current date and user principal name and host.
	 * @param principal The user principal
	 * @param hostname The host name
	 */
	@Override
	public void setUpdated(String principal, String hostname) {
		this.updatedOn = new Date();
		this.updatedBy = String.format("%s@%s", principal, hostname);
	}
}
