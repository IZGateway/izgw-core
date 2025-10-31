package gov.cdc.izgateway.model;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import gov.cdc.izgateway.common.Constants;
import gov.cdc.izgateway.common.HasDestinationUri;
import io.swagger.v3.oas.annotations.media.Schema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;

/**
 * Represents a destination endpoint in IZ Gateway, including facility, maintenance, credentials, and protocol details.
 * <p>
 * Provides methods to get and set destination properties, maintenance windows, credentials, and protocol information.
 * </p>
 */
public interface IDestination extends IEndpoint, HasDestinationUri, DbAudit {
	@SuppressWarnings("serial")
	static class Map extends MappableEntity<IDestination> {}
	static String ID_PATTERN = "^[-_\\p{Alnum}]+$";

	/** Version value for IIS endpoints using CDC Schema */
	public static final String IZGW_2011 = "2011";
	/** Version value for IIS endpoints using 2014 Schema (subset of HUB Schema w/o HubHeader) */
	public static final String IZGW_2014 = "2014";
	/** Version value for IIS endpoints using IZGW Hub Schema */
	public static final String IZGW_HUB = "HUB";
	/** Version value for ADS endpoints using DEX 1.0 Schema */
	public static final String IZGW_ADS_VERSION1 = "DEX1.0";
	/** Version value for ADS endpoints using DEX 2.0 Schema */
	public static final String IZGW_ADS_VERSION2 = "DEX2.0";
	/** Version value for ADS endpoints using NDLP 1.0 Schema (Azure with v1 Folder Structure) */
	public static final String IZGW_AZURE_VERSION1 = "V2022-12-31";
	/** Version value for ADS endpoints using NDLP 2.0 Schema (Azure with v2 Folder Structure) */
	public static final String IZGW_AZURE_VERSION2 = "V2022-12-31";

	/**
	 * Gets the facility ID for the destination.
	 * @return the facility ID
	 */
	String getFacilityId();

	/**
	 * Gets the destination identifier.
	 * @return the destination identifier
	 */
	IDestinationId getId();

	/**
	 * Gets the maintenance end date.
	 * @return the maintenance end date
	 */
	@JsonFormat(shape=Shape.STRING, pattern=Constants.TIMESTAMP_FORMAT) 
	Date getMaintEnd();

	/**
	 * Gets the maintenance reason.
	 * @return the maintenance reason
	 */
	String getMaintReason();

	/**
	 * Gets the maintenance start date.
	 * @return the maintenance start date
	 */
	@JsonFormat(shape=Shape.STRING, pattern=Constants.TIMESTAMP_FORMAT) 
	Date getMaintStart();

	/**
	 * Gets the MSH-22 value for HL7 messages.
	 * @return the MSH-22 value
	 */
	String getMsh22();

	/**
	 * Gets the MSH-3 value for HL7 messages.
	 * @return the MSH-3 value
	 */
	String getMsh3();

	/**
	 * Gets the MSH-4 value for HL7 messages.
	 * @return the MSH-4 value
	 */
	String getMsh4();

	/**
	 * Gets the MSH-5 value for HL7 messages.
	 * @return the MSH-5 value
	 */
	String getMsh5();

	/**
	 * Gets the MSH-6 value for HL7 messages.
	 * @return the MSH-6 value
	 */
	String getMsh6();

	/**
	 * Gets the password for the destination.
	 * @return the password
	 */
	String getPassword();
	
	/**
	 * Gets the password expiry date.
	 * @return the password expiry date
	 */
	Date getPassExpiry();

	/**
	 * Gets the RXA-11 value for HL7 messages.
	 * @return the RXA-11 value
	 */
	String getRxa11();

	/**
	 * Gets the username for the destination.
	 * @return the username
	 */
	String getUsername();
	
	void setDestUri(String destUri);

	void setDestVersion(String destVersion);

	void setFacilityId(String facilityId);

	void setId(IDestinationId id);

	@JsonIgnore
	void setJurisdictionId(int jurisdictionId);

	void setMaintEnd(Date maintEnd);

	void setMaintReason(String maintReason);

	void setMaintStart(Date maintStart);

	void setMsh22(String msh22);

	void setMsh3(String msh3);

	void setMsh4(String msh4);

	void setMsh5(String msh5);

	void setMsh6(String msh6);

	@JsonIgnore
	void setPassword(String password);
	
	void setPassExpiry(Date expiry);

	void setRxa11(String rxa11);

	@JsonIgnore
	void setUsername(String username);

	void setDestTypeId(int destType);

	boolean isUnderMaintenance();

	String getMaintenanceDetail();

	IDestination safeCopy();

	@DynamoDbIgnore
	@JsonIgnore
	@Schema(description = "True if this destination supports the original CDC 2011 Protocol", hidden=true)
	default boolean is2011() {
		return IZGW_2011.equals(getDestVersion());
	}

	@DynamoDbIgnore
	@JsonIgnore
	@Schema(description = "True if this destination supports the IZ Gateway 2014 Protocol", hidden=true)
	default boolean is2014() {
		String destVersion = getDestVersion();
		return StringUtils.isEmpty(destVersion) || IZGW_2014.equals(destVersion);
	}
	
	@DynamoDbIgnore
	@JsonIgnore
	@Schema(description = "True if this destination supports the IZ Gateway Hub Protocol", hidden=true)
	default boolean isHub() {
		return IZGW_HUB.equalsIgnoreCase(getDestVersion());
	}
	
	@DynamoDbIgnore
	@JsonIgnore
	@Schema(description = "True if this destination supports the CDC DEX Protocol", hidden=true)
	default boolean isDex() {
		String destVersion = getDestVersion();
		return IZGW_ADS_VERSION1.equals(destVersion) || IZGW_ADS_VERSION2.equals(destVersion);
	}
	
	@DynamoDbIgnore
	@JsonIgnore
	@Schema(description = "True if this destination supports the Azure Blob Storage Protocol", hidden=true)
	default boolean isAzure() {
		return IZGW_AZURE_VERSION1.equals(getDestVersion());
	}
	
	@JsonIgnore
	@Schema(description = "The destination id", hidden=true)
	@Override
	default String getDestinationId() {
		return getId().getDestId();
	}

	@JsonIgnore
	@Schema(description = "The destination uri", hidden=true)
	@Override
	default String getDestinationUri() {
		return getDestUri();
	}	
	
	/**
	 * Get an example destination with the specified id (used for swagger)
	 * @param iDestination The destination to turn into an example
	 * @param destId	The destId value to report in the example
	 * @return	An example destination object
	 */
	public static IDestination getExample(IDestination iDestination, String destId) {
		iDestination.getId().setDestId(destId);
		iDestination.setDestUri("https://example.com/dev/IISService");
		iDestination.setDestVersion("2011");
		iDestination.setUsername("username");
		iDestination.setPassword("password");
		iDestination.setJurisdictionId(1);
		iDestination.setMaintReason(null);
		iDestination.setMaintStart(null);
		iDestination.setMaintEnd(null);
		iDestination.setFacilityId(null);
		iDestination.setMsh3("IZGW");
		iDestination.setMsh4("IZGW");
		iDestination.setMsh5("IZGW");
		iDestination.setMsh6("IZGW");
		iDestination.setMsh22("IZGW");
		iDestination.setRxa11("IZGW");
		return iDestination;
	}


}