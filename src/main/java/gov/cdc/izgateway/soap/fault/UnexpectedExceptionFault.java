package gov.cdc.izgateway.soap.fault;

import gov.cdc.izgateway.model.RetryStrategy;
import lombok.Getter;

public class UnexpectedExceptionFault extends Fault {
	private static final long serialVersionUID = 1L;
	public static final String FAULT_NAME = "UnexpectedExceptionFault";
	private final Throwable fault;
	@Getter
	private final String summary;
    @Getter
    private final String detail;
    @Getter
    private final RetryStrategy retry;
    @Getter
	private final String diagnostics;
    
	public static UnexpectedExceptionFault devAction() {
		return new UnexpectedExceptionFault("Simulated Fault", new NullPointerException("This is a test, this is only a test"), "User requested that a fault be generated for interface testing.");
	}
	
    public UnexpectedExceptionFault(String message, String detail, Throwable fault, RetryStrategy retry, String diagnostics) {
    	super.initCause(fault);
        this.fault = fault;
        this.summary = message == null ? "Unexpected " + getFaultName() : message;
        if (detail == null) {
        	this.detail = fault == null ? null : fault.getMessage();
        } else {
        	this.detail = detail;
        }
        this.retry = retry == null ? RetryStrategy.CONTACT_SUPPORT : retry;
        this.diagnostics = diagnostics;
    }

    public UnexpectedExceptionFault(String message, Throwable fault, String diagnostics) {
    	this(null, message, fault, RetryStrategy.CONTACT_SUPPORT, diagnostics);
    }
    public UnexpectedExceptionFault(Throwable fault, String diagnostics) {
        this(null, "Unexpected Exception", fault, RetryStrategy.CONTACT_SUPPORT, diagnostics);
    }

    @Override
    public String getMessage() {
        if (getDetail() == null) {
            return getSummary();
        }
        return getSummary() + ": " + getDetail();
    }

    @Override
    public String getCode() {
        return "003";
    }

    @Override
    public String getFaultName() {
        return fault == null ? UnexpectedExceptionFault.class.getSimpleName() : fault.getClass().getSimpleName();
    }
}