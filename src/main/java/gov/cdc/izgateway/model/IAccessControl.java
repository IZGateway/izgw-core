package gov.cdc.izgateway.model;

public interface IAccessControl {

	String getCategory();

	String getMember();

	String getName();

	boolean isAllowed();

	void setAllowed(boolean allowed);

	void setCategory(String category);

	void setMember(String member);

	void setName(String name);

}