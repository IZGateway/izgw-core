package gov.cdc.izgateway.soap.message;

public interface HasCredentials extends SoapMessage.Request {
	String getUsername();
	void setUsername(String username);
	String getPassword();
	void setPassword(String password);
}
