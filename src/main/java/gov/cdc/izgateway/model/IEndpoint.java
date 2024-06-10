package gov.cdc.izgateway.model;

public interface IEndpoint {

	String getDestId();

	String getDestUri();

	String getDestType();

	int getDestTypeId();

	String getJurisdictionName();

	String getJurisdictionDesc();

	int getJurisdictionId();

	String getDestVersion();

}