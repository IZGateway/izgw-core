package gov.cdc.izgateway.service;

import java.util.List;

import gov.cdc.izgateway.model.IMessageHeader;

public interface IMessageHeaderService {

	void refresh();

	IMessageHeader findByMsgId(String msgId);

	List<IMessageHeader> getMessageHeaders(List<String> mshList);

	List<IMessageHeader> getAllMessageHeaders();

	String getSourceType(String... idList);

	IMessageHeader saveAndFlush(IMessageHeader h);
	
	void delete(String id);

}