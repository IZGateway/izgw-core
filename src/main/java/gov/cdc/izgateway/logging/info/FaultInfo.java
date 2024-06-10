package gov.cdc.izgateway.logging.info;

import java.io.Serializable;

import lombok.Data;

@Data class FaultInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	private final String Code;		// NOSONAR Names are OK
	private final String Reason;	// NOSONAR Names are OK
	private final FaultInfo Detail;	// NOSONAR Names are OK
	private String Retry;			// NOSONAR Names are OK
	private String Summary;			// NOSONAR Names are OK
	private String Diagnostics;		// NOSONAR Names are OK
}