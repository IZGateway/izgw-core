package gov.cdc.izgateway.soap.fault;

import gov.cdc.izgateway.logging.info.EndPointInfo;
import gov.cdc.izgateway.model.IDestination;
import gov.cdc.izgateway.model.RetryStrategy;
import lombok.Getter;

/**
 * SecurityFault is used to report issues regarding the security of connections or messaging.
 * 
 * @author Audacious Inquiry
 *
 */
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
    		new MessageSupport(FAULT_NAME, "63", "Decryption Failure", "Failure decrypting password for destination.", 
					"The password used to connect to the specified destination could not be decrypted.", 
					RetryStrategy.CONTACT_SUPPORT)
    	};
    static {
    	MessageSupport.registerMessageSupport(MESSAGE_TEMPLATES[0]);
    	MessageSupport.registerMessageSupport(MESSAGE_TEMPLATES[1]);
    }
    
    private SecurityFault(MessageSupport s, Throwable cause, EndPointInfo endpoint) {
        super(s, cause);
        this.endpoint = endpoint;
    }

    /**
     * Report a general security issue not elsewhere specified below.
     * @param summary	The summary message, should be the same in all cases where the issue is the same
     * @param detail	The detailed part of the message, provides specifics for each case
     * @param cause		The cause of the exception
     * @return	A new security fault ready to be thrown
     */
    public static SecurityFault generalSecurity(String summary, String detail, Throwable cause) {
    	return new SecurityFault(MESSAGE_TEMPLATES[0].copy().setSummary(summary, detail), cause, null);
    }


    /**
     * Report a detected potential attack from a message sender or from a recipient's response
     * @param detail	More details about what was found
     * @param endpoint	The endpoint that was the source of the suspected attack
     * @return	A new security fault ready to be thrown
     */
    public static SecurityFault sourceAttack(String detail, EndPointInfo endpoint) {
    	return new SecurityFault(MESSAGE_TEMPLATES[1].copy().setDetail(detail), null, endpoint);
	}
    
    /**
     * Report an attempt to access information by a blacklisted user
     * @param endpoint	The endpoint that is attempting the access
     * @return	A new security fault ready to be thrown
     */
    public static SecurityFault userBlacklisted(EndPointInfo endpoint) {
        return new SecurityFault(MESSAGE_TEMPLATES[2].copy().setDetail(endpoint.getCommonName()), null, endpoint);
	}
    
    /**
     * Report a failure to decrypt a destination password
     * @param dest	The destination whose password could not be decrypted
     * @param cause	The cause of the failure
     * @return	A new security fault ready to be thrown
     */
    public static SecurityFault decryptionFailure(IDestination dest, Throwable cause) {
		return new SecurityFault(MESSAGE_TEMPLATES[3].copy()
				.setDetail(String.format("Decryption failure on %s (%s)", dest.getId().getDestId(), dest.getDestUri())), 
				cause, null);
    }
}
