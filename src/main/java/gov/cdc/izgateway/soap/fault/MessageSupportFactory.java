package gov.cdc.izgateway.soap.fault;

import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.cdc.izgateway.logging.markers.Markers2;
import gov.cdc.izgateway.model.RetryStrategy;
import gov.cdc.izgateway.soap.message.SoapProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Load A message support class from SoapProperties, or get the previously loaded one.
 */
@Component
@Slf4j
public class MessageSupportFactory {
	private static SoapProperties soapProperties = null; 
	private Map<String, MessageSupport> messages;
	
	private static class MissingMessageException extends Exception {
		private static final long serialVersionUID = 1L;
		@Getter
		private final String error;
		MissingMessageException(String faultName, String messageName, String error) {
			super(String.format("Missing message for Fault: %s, Message: %s", faultName, messageName));
			this.error = error == null ? "" : error;
		}
	}
	
	public MessageSupportFactory(@Autowired SoapProperties soapProperties) {
		MessageSupportFactory.soapProperties = soapProperties;
	}
	
	public MessageSupport getMessageSupport(Class<? extends Fault> clazz, String messageName) {
		String key = clazz.getSimpleName() + messageName;
		MessageSupport m = messages.get(key);
		if (m != null) {
			return m;
		}
		
		try {
			m = findMessage(clazz.getSimpleName(), messageName);
		} catch (MissingMessageException e) {
			// The logs will report where this exception occured so it can be fixed. 
			log.warn(Markers2.append(e), e.getMessage());
			m = buildPlaceholderMessage(clazz.getSimpleName(), messageName, e.getError());
		}
		messages.put(key, m);
		return m;
	}
	public MessageSupport getMessageSupport(Class<? extends Fault> clazz, String messageName, String detail) {
		return getMessageSupport(clazz, messageName).setDetail(detail);
	}
	public MessageSupport getMessageSupport(Class<? extends Fault> clazz, String messageName, Throwable cause) {
		return getMessageSupport(clazz, messageName).setDetail(cause);
	}
	
	private MessageSupport findMessage(String faultName, String messageName) throws MissingMessageException {
		if (soapProperties.getFaults() == null) {
			throw new MissingMessageException(faultName, messageName, "Configuration error in soap.faults");
		}
		Map<String, Map<String, String>> faultMessages = soapProperties.getFaults().get(faultName);
		if (faultMessages == null) {
			throw new MissingMessageException(faultName, messageName, "Missing soap.faults." + faultName);
		}
		
		Map<String, String> faultMessage = faultMessages.get(messageName);
		if (faultMessage == null) {
			throw new MissingMessageException(faultName, messageName, "Missing soap.faults." + faultName + "." + messageName);
		}
		
		return constructMessage(faultName, messageName, faultMessage);
	}

	private MessageSupport constructMessage(String faultName, String messageName, Map<String, String> faultMessage) {
		String code = ObjectUtils.defaultIfNull(faultMessage.get("code"), "unknown");
		String summary = faultMessage.get("summary");
		if (summary == null) {
			summary = messageName.replaceAll("([A-Z]+)", "$1 ");
		}
		String detail = faultMessage.get("detail");
		String diagnostics = faultMessage.get("detail");
		String retry = faultMessage.get("retry");
		RetryStrategy retryStrategy = RetryStrategy.CONTACT_SUPPORT;
		if (retry != null) {
			try {
				retryStrategy = Enum.valueOf(RetryStrategy.class, retry);
			} catch (IllegalArgumentException ex) {
				// Swallow the exception and Leave it set as CONTACT_SUPPORT
			}
		}

		return new MessageSupport(faultName, code, summary, detail, diagnostics, retryStrategy);
	}
	
	private MessageSupport buildPlaceholderMessage(String faultName, String messageName, String error) {
		String summary = messageName.replaceAll("([A-Z]+)", "$1 ");
		return new MessageSupport(faultName, "unknown", summary, null, 
				"No diagnostics are not available. " + error, 
				RetryStrategy.CONTACT_SUPPORT);
	}
}