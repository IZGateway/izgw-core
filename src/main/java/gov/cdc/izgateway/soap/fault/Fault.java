package gov.cdc.izgateway.soap.fault;

import gov.cdc.izgateway.model.RetryStrategy;

public class Fault extends Exception implements FaultSupport {
	private static final long serialVersionUID = 1L;
    protected final MessageSupport messageSupport;

    protected Fault() {
    	messageSupport = null;
    }
    protected Fault(MessageSupport messageSupport) {
    	super(messageSupport.getMessage());
    	this.messageSupport = messageSupport;
    }
    
    protected Fault(MessageSupport messageSupport, Throwable faultCause) {
    	super(messageSupport.getMessage(), faultCause);
    	this.messageSupport = messageSupport;
    }
    
    protected Fault(MessageSupport messageSupport, String detail) {
		this(messageSupport.setDetail(detail));
	}
	
    protected Fault(MessageSupport messageSupport, String detail, Throwable faultCause) {
		this(messageSupport.setDetail(detail), faultCause);
	}

	@Override
	public String getSummary() {
		return messageSupport.getSummary();
	}

	@Override
	public String getDetail() {
		return messageSupport.getDetail();
	}

	@Override
	public String getDiagnostics() {
		return messageSupport.getDiagnostics();
	}

	@Override
	public String getCode() {
		return messageSupport.getCode();
	}

	@Override
	public RetryStrategy getRetry() {
		return messageSupport.getRetry();
	}

	@Override
	public String getFaultName() {
		return this.getClass().getSimpleName();
	}

}
