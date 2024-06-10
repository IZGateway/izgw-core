package gov.cdc.izgateway.model;

public interface IDestinationId {

	String getDestId();

	int getDestType();

	void setDestId(String destId);

	IDestinationId copy();

	void setDestType(String destType);

	void setDestType(int destType);
}