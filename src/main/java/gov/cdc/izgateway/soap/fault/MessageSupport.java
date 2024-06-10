package gov.cdc.izgateway.soap.fault;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import gov.cdc.izgateway.model.RetryStrategy;
import lombok.Data;

@Data
public final class MessageSupport implements FaultSupport, Serializable {
    private static final long serialVersionUID = 1L;
    private static Map<String, MessageSupport> templates = new LinkedHashMap<>();
    
    private final String diagnostics;
    private final RetryStrategy retry;
    private String summary;
    private String detail;
    private String code;
    private String faultName;
    private Object extra;		// NOSONAR Cannot make arbitrary object serializable
    
    public MessageSupport(String faultName, String code, String summary, String detail, String diagnostics, RetryStrategy retryStrategy, Object extra) {
    	this.faultName = faultName;
        this.summary = summary;
        this.detail = detail;
        this.diagnostics = diagnostics;
        this.retry = retryStrategy == null ? RetryStrategy.CONTACT_SUPPORT : retryStrategy;
        this.code = code;
    	this.extra = extra;
    }
    public MessageSupport(String faultName, String code, String summary, String detail, String diagnostics, RetryStrategy retryStrategy) {
    	this(faultName, code, summary, detail, diagnostics, retryStrategy, null);
    }
    
    public static void registerMessageSupport(MessageSupport template) {
    	templates.put(template.faultName + template.code, template);
    }

	public static FaultSupport getTemplate(String faultCode, String faultName) {
		return templates.get(faultName + faultCode);
	}
    
    MessageSupport(String faultName, String code, String message) {
    	MessageSupport t = templates.get(faultName + code);
		this.faultName = faultName;
		this.detail = message;
    	if (t == null) {
    		this.summary = "Unknown fault code: " + code;
    		this.diagnostics = "The root cause of this fault is not recognized.";
    		this.retry = RetryStrategy.CONTACT_SUPPORT;
    		this.code = code;
    	} else {
    		this.summary = t.summary; 
    		this.diagnostics = t.diagnostics;
    		this.retry = t.retry;
    		this.code = t.code;
    	}
    }
    
    MessageSupport(String faultName, String code, String summary, String message) {
    	MessageSupport t = templates.get(faultName + code);
		this.faultName = faultName;
		this.summary = summary;
		this.detail = message;
    	if (t == null) {
    		this.diagnostics = "The root cause of this fault is not recognized.";
    		this.retry = RetryStrategy.CONTACT_SUPPORT;
    		this.code = code;
    	} else {
    		this.diagnostics = t.diagnostics;
    		this.retry = t.retry;
    		this.code = t.code;
    	}
    }

    public MessageSupport copy() {
        return new MessageSupport(faultName, code, summary, detail, diagnostics, retry);
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return detail == null ? summary : (summary + ": " + detail);
    }

    public MessageSupport setSummary(String summary, String detail) {
    	MessageSupport m = this.copy();
        m.summary = summary;
        m.detail = detail;
        return m;
    }
    public MessageSupport setDetail(String detail) {
    	MessageSupport m = this.copy();
        m.detail = detail;
        return m;
    }

    public MessageSupport setDetail(Throwable rootCause) {
    	if (rootCause != null) {
    		return setDetail(rootCause.getMessage());
    	}
    	return this;
    }
}