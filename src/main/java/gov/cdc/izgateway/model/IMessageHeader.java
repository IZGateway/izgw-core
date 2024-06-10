package gov.cdc.izgateway.model;

public interface IMessageHeader {
	@SuppressWarnings("serial")
	static class Map extends MappableEntity<IMessageHeader> {}

	String getDestId();

	String getFacilityId();

	String getIis();

	String getMsh();

	String getPassword();

	String getSourceType();

	String getUsername();

	void setDestId(String destId);

	void setFacilityId(String facilityId);

	void setIis(String iis);

	void setMsh(String msh);

	void setPassword(String password);

	void setSourceType(String sourceType);

	void setUsername(String username);

}