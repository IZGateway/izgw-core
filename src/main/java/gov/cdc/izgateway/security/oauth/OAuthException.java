package gov.cdc.izgateway.security.oauth;

import com.fasterxml.jackson.annotation.JsonIgnore;

@SuppressWarnings("serial")
public class OAuthException extends Exception {
    final String error;
    
    public OAuthException(String error, String error_description) {
        super(error_description);
        this.error = error;
    }
    
    @JsonIgnore
    public ErrorObject getOAuthError() {
        return new ErrorObject(this);
    }
}