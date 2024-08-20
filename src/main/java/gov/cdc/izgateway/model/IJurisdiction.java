package gov.cdc.izgateway.model;

public interface IJurisdiction {

	String getDescription();

	int getJurisdictionId();

	String getName();

	String getPrefix();

	void setDescription(String description);

	void setJurisdictionId(int jurisdictionId);

	void setName(String name);

	void setPrefix(String prefix);

}