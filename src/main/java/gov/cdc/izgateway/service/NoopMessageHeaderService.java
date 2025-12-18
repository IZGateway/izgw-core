package gov.cdc.izgateway.service;

import gov.cdc.izgateway.model.IMessageHeader;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * No-op implementation of IMessageHeaderService to be used when no other implementation is provided.
 */
@Service
@ConditionalOnMissingBean(IMessageHeaderService.class)
public class NoopMessageHeaderService implements IMessageHeaderService {
    @Override
    public void refresh() {
      // This is a no-op
    }

    @Override
    public IMessageHeader findByMsgId(String msgId) {
        return null;
    }

    @Override
    public List<IMessageHeader> getMessageHeaders(List<String> mshList) {
        return List.of();
    }

    @Override
    public List<IMessageHeader> getAllMessageHeaders() {
        return List.of();
    }

    @Override
    public String getSourceType(String... idList) {
        return "";
    }

    @Override
    public IMessageHeader saveAndFlush(IMessageHeader h) {
        return null;
    }

	@Override
	public void delete(String id) {
		// This is a no-op
	}
}
