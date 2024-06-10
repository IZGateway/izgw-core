package gov.cdc.izgateway.security.oauth;

import java.util.concurrent.ConcurrentHashMap;

public class SimpleTokenStore {

    private final ConcurrentHashMap<String, AccessToken> tokens = new ConcurrentHashMap<>();

    public AccessToken createToken() {
        return save(new AccessToken());
    }

    protected AccessToken save(AccessToken token) {
        tokens.put(token.getAccessToken(), token);
        tokens.put(token.getRefreshToken(), token);
        return token;
    }
    /**
     * Remove all saved tokens in a token store.
     * Used to erase any memory of previously retrieved tokens for debugging purposes.
     */
    public void clearTokens() {
    	tokens.clear();
    }
    public AccessToken getAccess(String accessToken2) throws OAuthException {
        AccessToken tok = tokens.get(accessToken2);
        if (tok == null) {
            throw new OAuthException("invalid_grant", "Invalid access token");
        } else if (!tok.getAccessToken().equals(accessToken2)) {
            throw new OAuthException("invalid_grant", "Not an access token");
        }
        if (tok.getAccessToken().equals(accessToken2) && tok.isValid()) {
            return tok;
        }
        throw new OAuthException("invalid_grant", "Access token has expired");
    }

    public AccessToken getRefresh(String refreshToken2) throws OAuthException {
        AccessToken tok = tokens.get(refreshToken2);
        if (tok == null) {
            throw new OAuthException("invalid_grant", "Invalid refresh token");
        } else if (!tok.getRefreshToken().equals(refreshToken2)) {
            throw new OAuthException("invalid_grant", "Not a refresh token");
        }
        if (tok.getRefreshToken().equals(refreshToken2) && tok.isRefreshable()) {
            return tok;
        }
        throw new OAuthException("invalid_grant", "Refresh token has expired");
    }

}
