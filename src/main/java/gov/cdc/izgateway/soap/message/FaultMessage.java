package gov.cdc.izgateway.soap.message;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import gov.cdc.izgateway.common.HasDestinationId;
import gov.cdc.izgateway.common.HasDestinationUri;
import gov.cdc.izgateway.soap.fault.DestinationConnectionFault;
import gov.cdc.izgateway.soap.fault.Fault;
import gov.cdc.izgateway.soap.fault.HubClientFault;
import gov.cdc.izgateway.soap.fault.MessageTooLargeFault;
import gov.cdc.izgateway.soap.fault.UnknownDestinationFault;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Interface for translation of faults to SOAP Messages */
@Data
@EqualsAndHashCode(callSuper=true)
public class FaultMessage extends SoapMessage {
	private static final long serialVersionUID = 1L;
	protected static final List<String> HUB_FAULTS = Arrays.asList(
			HubClientFault.class.getSimpleName(), 
			DestinationConnectionFault.class.getSimpleName(), 
			UnknownDestinationFault.class.getSimpleName()
		);
	public FaultMessage(SoapMessage that, String schema, boolean isUpgradeOrSchemaChange) {
		super(that, schema, isUpgradeOrSchemaChange);
	}
	
	public FaultMessage(String schema) {
		setSchema(schema);
	}
	
	public FaultMessage(Fault fault, String schema) {
		setSchema(schema);
		this.fault = fault;
		// Handle cases common to all faults
		this.code = fault.getCode();
		this.detail = fault.getDetail();
		this.diagnostics = fault.getDiagnostics();
		this.faultName = fault.getFaultName();
		this.retry = fault.getRetry().name();
		this.summary = fault.getSummary();
		this.reason = fault.getMessage();
		
		if (fault instanceof HasDestinationUri furi) {
			this.destinationId = furi.getDestinationId();
			this.destinationUri = furi.getDestinationUri();
		} else if (fault instanceof HasDestinationId fid) {
			this.destinationId = fid.getDestinationId();
		} 
		if (fault instanceof MessageTooLargeFault fmtl) {
			this.size = Long.toString(fmtl.getSize());
			this.maxSize = Long.toString(fmtl.getMaxSize());
		} 
		if (fault instanceof HubClientFault fhcf) {
			this.original = fhcf.getOriginalBody();
			this.statusCode = Integer.toString(fhcf.getStatusCode());
		}
	}
	
	// All Faults
	@JsonIgnore
	private Throwable fault;
	private String faultName = "Fault";
	private String code;
	private String eventId;
	private String retry;
	private String summary;
	private String detail;
	private String diagnostics;
	private String reason;
	
	// MessageTooLargeFault
	private String size;
	private String maxSize;
	
	// DestinationConnectionFault, UnknownDestinationFault and HubClientFault
	private String destinationId;
	private String destinationUri;
	
	// Only for HubClientFault
	private String original;
	private String statusCode;
	
	public boolean isHubFault() {
		return HUB_FAULTS.contains(getFaultName());
	}
}	
