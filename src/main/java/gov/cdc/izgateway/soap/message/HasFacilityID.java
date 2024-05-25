package gov.cdc.izgateway.soap.message;

public interface HasFacilityID extends HasCredentials {
	String getFacilityID();
	void setFacilityID(String facilityID);
}
