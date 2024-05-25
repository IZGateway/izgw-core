package gov.cdc.izgateway.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

// TODO: Add schema documentation
@Data
public class ErrorResponse {
    @JsonProperty
    private final String eventId;
    @JsonProperty
    protected String message;
    @JsonProperty("error")
    protected String summary;
    @JsonProperty("error_description")
    protected String detail;
    @JsonProperty("diagnostics")
	protected String diagnostics;


	public ErrorResponse(String eventId, String message, String summary, String detail) {
		this.eventId = eventId;
		this.message = message;
		this.summary = summary;
		this.detail = detail;
	}
}
