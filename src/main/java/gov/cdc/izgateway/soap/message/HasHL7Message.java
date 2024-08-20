package gov.cdc.izgateway.soap.message;

public interface HasHL7Message {
	String getHl7Message();
	void setHl7Message(String hl7Message);
	boolean isCdataWrapped();
	void setCdataWrapped(boolean isCdataWrapped);
}
