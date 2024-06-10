package gov.cdc.izgateway.soap.fault;

import gov.cdc.izgateway.logging.info.EndPointInfo;
import gov.cdc.izgateway.model.RetryStrategy;
import lombok.Getter;

public class SecurityFault extends Fault {
	private static final long serialVersionUID = 1L;
	@Getter 
	private final EndPointInfo endpoint;
    private static final String FAULT_NAME = "SecurityFault";
    
    private static final MessageSupport[] MESSAGE_TEMPLATES = { 
    		new MessageSupport(FAULT_NAME, "60", "Security Exception", null, "Security Exception", RetryStrategy.CORRECT_MESSAGE),
    		new MessageSupport(FAULT_NAME, "61", "Source Attack Exception", "IZ Gateway received a message containing content suggesting the source of the message has been compromised", 
    				"A message was sent containing code that appears to be trying to infect the receiver or downstream recipients. This source has been blocked and cannot send or receive messages "
    				+ "to or from IZ Gateway until it has been cleared by support.", RetryStrategy.CONTACT_SUPPORT),
    		new MessageSupport(FAULT_NAME, "62", "User Blacklisted", "IZ Gateway received a message containing content suggesting the source of the message has been compromised", 
    				"A message was sent containing code that appears to be trying to infect the receiver or downstream recipients. This source has been blocked and cannot send or receive messages "
    				+ "to or from IZ Gateway until it has been cleared by support.", RetryStrategy.CONTACT_SUPPORT),
    	};
    static {
    	MessageSupport.registerMessageSupport(MESSAGE_TEMPLATES[0]);
    	MessageSupport.registerMessageSupport(MESSAGE_TEMPLATES[1]);
    }
    
    private SecurityFault(MessageSupport s, Throwable cause, EndPointInfo endpoint) {
        super(s, cause);
        this.endpoint = endpoint;
    }

    public static SecurityFault generalSecurity(String summary, String detail, Throwable cause) {
    	return new SecurityFault(MESSAGE_TEMPLATES[0].copy().setSummary(summary, detail), cause, null);
    }


    public static SecurityFault sourceAttack(String detail, EndPointInfo endpoint) {
    	return new SecurityFault(MESSAGE_TEMPLATES[1].copy().setDetail(detail), null, endpoint);
	}
    
    public static SecurityFault userBlacklisted(EndPointInfo endpoint) {
        return new SecurityFault(MESSAGE_TEMPLATES[2].copy().setDetail(endpoint.getCommonName()), null, endpoint);
	}
}
