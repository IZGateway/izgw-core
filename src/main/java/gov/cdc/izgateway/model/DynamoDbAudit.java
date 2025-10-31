package gov.cdc.izgateway.model;

import java.util.Date;

import gov.cdc.izgateway.model.DbAudit;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base class to provide audit fields for DynamoDB entities.
 * @author Audacious Inquiry
 */
@Data
@NoArgsConstructor
public abstract class DynamoDbAudit implements DbAudit {
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
	Date createdOn;
	Date updatedOn;
	String createdBy;
	String updatedBy;
}
