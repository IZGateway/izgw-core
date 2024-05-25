package gov.cdc.izgateway.soap.fault;

import gov.cdc.izgateway.common.HasDestinationId;
import gov.cdc.izgateway.model.RetryStrategy;
import lombok.Getter;

public class UnknownDestinationFault extends Fault implements HasDestinationId {
    private static final long serialVersionUID = 1L;
    public static final String FAULT_NAME = "UnknownDestinationFault";
    private static final MessageSupport[] MESSAGE_TEMPLATES = {
        new MessageSupport(FAULT_NAME, "40", "Missing HubRequestHeader", null,
            "The Request to IZ Gateway is missing the required <iishub:HubRequestHeader> or <iishub:DestinationId> element in the <soap:Header>.", RetryStrategy.CORRECT_MESSAGE),
        new MessageSupport(FAULT_NAME, "41", "IIS Not Registered", null,
            "The Destination IIS is not registered for use in IZ Gateway for the requested operation. If this IIS should be registered, "
            + "contact IZ Gateway support.", RetryStrategy.CONTACT_SUPPORT),
        new MessageSupport(FAULT_NAME, "42", "Invalid Destination ID", null,
            "The Destination IIS is not valid for use in IZ Gateway.", RetryStrategy.CORRECT_MESSAGE)
    };
    @Getter 
    private final String destinationId;
    public UnknownDestinationFault(int index, String destId, String message, Throwable cause) {
        super(MESSAGE_TEMPLATES[index], message, cause);
    	this.destinationId = destId;
    }
    
    public static UnknownDestinationFault missingDestination(String message) {
        return new UnknownDestinationFault(0, "_null_", message, null);
    }
    
    public static UnknownDestinationFault unknownDestination(String destId, String message, Throwable cause) {
        return new UnknownDestinationFault(1, destId, message, cause);
    }

    public static UnknownDestinationFault invalidDestination(String destId, String message) {
        return new UnknownDestinationFault(2, destId == null ? "_null_" : destId, message, null);
    }
    
    public static UnknownDestinationFault devAction(String destId) {
    	return unknownDestination(destId, "Simulated UnknownDestinationFault", new Exception("There is no reason for this"));
    }
    	 
}
