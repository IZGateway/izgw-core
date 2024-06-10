package gov.cdc.izgateway.soap.fault;

import gov.cdc.izgateway.model.RetryStrategy;
import lombok.Getter;

public class MessageTooLargeFault extends Fault {
	private static final long serialVersionUID = 1L;
    public enum Direction {
        REQUEST, RESPONSE
    }
    public static final String FAULT_NAME = "MessageTooLargeFault";
    
    @Getter
    private final long size;
    @Getter
    private final long maxSize; 

    private static final MessageSupport[] MESSAGE_TEMPLATES = {
        new MessageSupport(FAULT_NAME, "30", "Request Message Too Large", null,
            "The Request is too large to process.",
            RetryStrategy.CORRECT_MESSAGE),
        new MessageSupport(FAULT_NAME, "31", "Response Message Too Large", null,
            "The Response is too large to process.",
            RetryStrategy.CONTACT_SUPPORT)
    };
    
    static {
    	for (MessageSupport m: MESSAGE_TEMPLATES) {
    		MessageSupport.registerMessageSupport(m);
    	}
    }

    public MessageTooLargeFault(Direction direction, long maxSize, long actualSize) {
        super(MESSAGE_TEMPLATES[direction.ordinal()].setDetail(String.format("%d exceeds %d", actualSize, maxSize)));
        this.size = actualSize;
        this.maxSize = maxSize;
    }
    
    public static MessageTooLargeFault devAction() {
    	return new MessageTooLargeFault(Direction.REQUEST, 0, 1);
    }
}
