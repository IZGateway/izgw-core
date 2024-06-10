package gov.cdc.izgateway.service;

import java.util.List;

import gov.cdc.izgateway.model.IDestination;

public interface IDestinationService {

	String getServerName();

	int getServerPort();

	int getLbPort();

	String getServerProtocol();

	void refresh();

	List<IDestination> getAllDestinations();

	IDestination findByDestId(String destId);

	void saveAndFlush(IDestination dest);

	String publicUrl(String uri);

	String localUrl(String uri);

	String serverOf(String uri);

	void clearMaintenance(IDestination dest);

	IDestination getExample(String destinationId);

}