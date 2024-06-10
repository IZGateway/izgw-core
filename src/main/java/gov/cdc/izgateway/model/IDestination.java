package gov.cdc.izgateway.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import gov.cdc.izgateway.common.Constants;
import gov.cdc.izgateway.common.HasDestinationUri;

public interface IDestination extends IEndpoint, HasDestinationUri {
	@SuppressWarnings("serial")
	static class Map extends MappableEntity<IDestination> {}
	static String ID_PATTERN = "^[-_\\p{Alnum}]+$";

	String getFacilityId();

	IDestinationId getId();

	Date getMaintEnd();

	String getMaintReason();

	Date getMaintStart();

	String getMsh22();

	String getMsh3();

	String getMsh4();

	String getMsh5();

	String getMsh6();

	String getPassword();

	String getRxa11();

	String getUsername();

	void setDestUri(String destUri);

	void setDestVersion(String destVersion);

	void setFacilityId(String facilityId);

	void setId(IDestinationId id);

	@JsonIgnore
	void setJurisdictionId(int jurisdictionId);

	void setMaintEnd(Date maintEnd);

	void setMaintReason(String maintReason);

	@JsonFormat(shape=Shape.STRING, pattern=Constants.TIMESTAMP_FORMAT) 
	void setMaintStart(Date maintStart);

	void setMsh22(String msh22);

	void setMsh3(String msh3);

	void setMsh4(String msh4);

	void setMsh5(String msh5);

	void setMsh6(String msh6);

	@JsonIgnore
	void setPassword(String password);

	void setRxa11(String rxa11);

	@JsonIgnore
	void setUsername(String username);

	void setDestTypeId(int destType);

	boolean isUnderMaintenance();

	String getMaintenanceDetail();

	IDestination safeCopy();

	boolean is2011();

	boolean is2014();

	boolean isHub();

	boolean isDex();

	String getDestinationUri();

}