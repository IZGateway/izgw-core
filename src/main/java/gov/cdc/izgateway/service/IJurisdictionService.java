package gov.cdc.izgateway.service;

import gov.cdc.izgateway.model.IJurisdiction;

public interface IJurisdictionService {

	IJurisdiction getJurisdiction(int jurisdictionId);

	void refresh();

}