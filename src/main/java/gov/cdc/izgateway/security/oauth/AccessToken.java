package gov.cdc.izgateway.security.oauth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

import java.util.UUID;

/**
 * A simple OAuth Access Token.
 * This is just a POJO supporint an access token.
 */
@Data
public class AccessToken {
	/** Default scope for newly created token */
	private static final String DEFAULT_SCOPE = "create read";
	/** Lag time for verify check to avoid starting a transaction using a token just about to expire
	 *  This avoids cases of verifying that a token is valid, but then by the time the connection is made
	 *  and it is used, it has expired. 
	 */
    private static final long LAG_TIME = 5000; 
    @JsonProperty("access_token")
    private String accessToken = UUID.randomUUID().toString();
    @JsonProperty("token_type")
    private String tokenType = "Bearer";
    @JsonProperty("expires_in")
    private int expiresIn = 30;
    @JsonProperty("refresh_token")
    private String refreshToken = UUID.randomUUID().toString();
    @JsonProperty("scope")
    private String scope = DEFAULT_SCOPE;
    private long accessExpirationTime = System.currentTimeMillis() + expiresIn * 1000;
    private long refreshExpirationTime = System.currentTimeMillis() + expiresIn * 2000;
    
    public void setExpiresIn(int seconds) {
    	long time = System.currentTimeMillis();
    	expiresIn = seconds;
    	accessExpirationTime = time + expiresIn * 1000l;
    	// We don't really know, this is just a guess.
    	refreshExpirationTime = time + expiresIn * 2000l;
    }
    
    @JsonIgnore
    public boolean isValid() {
    	// Give us some lag time so that we don't use a token that's going to expire before we make a connection
        return accessExpirationTime > System.currentTimeMillis() + LAG_TIME;
    }
    
    @JsonIgnore
    public boolean isRefreshable() {
        return refreshExpirationTime > System.currentTimeMillis();
    }
}