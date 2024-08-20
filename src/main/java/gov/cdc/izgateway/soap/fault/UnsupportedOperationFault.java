package gov.cdc.izgateway.soap.fault;

import gov.cdc.izgateway.model.RetryStrategy;

public class UnsupportedOperationFault extends Fault {
    private static final long serialVersionUID = 1L;
    public static final String FAULT_NAME = "UnsupportedOperationFault";
    private static final MessageSupport MESSAGE_TEMPLATE =
        new MessageSupport(FAULT_NAME, "50", "Unsupported Operation", null,
            "An attempt was made to call an operation that is not supported by this application", RetryStrategy.CORRECT_MESSAGE);
    static {
    	MessageSupport.registerMessageSupport(MESSAGE_TEMPLATE);
    }
    
    public UnsupportedOperationFault(String detail, Throwable cause) {
        super(MESSAGE_TEMPLATE.setDetail(detail), cause);
    }
    
    public UnsupportedOperationFault(Exception e) {
    	this(e.getMessage(), e);
	}
    
    public static UnsupportedOperationFault devAction() {
    	return new UnsupportedOperationFault("IZ Gateway cannot support simulating an unsupported operation (or can it)?", null);
    }
}
