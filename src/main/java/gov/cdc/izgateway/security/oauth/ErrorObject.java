package gov.cdc.izgateway.security.oauth;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorObject implements Serializable {
	private static final long serialVersionUID = 1L;
    private final OAuthException oAuthException;

    /**
     * @param oAuthException
     */
    ErrorObject(OAuthException oAuthException) {
        this.oAuthException = oAuthException;
    }

    @JsonProperty("error")
    public String getError() {
        return this.oAuthException.error;
    }
    
    @JsonProperty("error_description")
    public String getErrorDescription() {
        return this.oAuthException.getMessage();
    }
}