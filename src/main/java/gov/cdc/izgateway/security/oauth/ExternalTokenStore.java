package gov.cdc.izgateway.security.oauth;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.cdc.izgateway.logging.markers.Markers2;
import gov.cdc.izgateway.security.ClientTlsSupport;
import gov.cdc.izgateway.utils.CapturingSSLSocketFactory;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.ws.http.HTTPException;

@Slf4j
@Data
@EqualsAndHashCode(callSuper=false)
public class ExternalTokenStore extends SimpleTokenStore {
	private static final String OAUTH_ENDPOINT = "oauthEndpoint";
	private ClientTlsSupport tlsSupport;
    private URL url;
    private String username;
    private String password;
    private AccessToken accessToken;
    private boolean usingQueryParameters;
    private boolean debugging;
    private int numRetries = 2;
    
    private static Map<String, ExternalTokenStore> stores = Collections.synchronizedMap(new TreeMap<>());
    
    public interface RequestWriter {
    	   void write(HttpsURLConnection con, AccessToken t) throws IOException;
    }
    
    public ExternalTokenStore(URL url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }
    
    public static ExternalTokenStore getTokenStore(URL url, String username, String password) { 
    	ExternalTokenStore t = stores.computeIfAbsent(url.toString(), u -> new ExternalTokenStore(url, username, password));
    	// If the username or password changed since we created it, update them.
    	if (!t.username.equals(username)) {
    		t.username = username;
    	}
    	if (!t.password.equals(password)) {
    		t.password = password;
    	}
    	return t;
    }
    
    /**
     * Erase any memory of existing tokens in the specified token store
     * @param url	The URL associated with the token store.
     */
    public static void clearTokenStore(String url) {
    	ExternalTokenStore t = stores.get(url);
    	if (t != null) {
    		t.clearTokens();
    	}
    }
    
    // Force refresh of all stored tokens.
	public static void refreshAllStores() {
		stores.clear();
	}
    
    @SuppressWarnings("serial")
    public static class OAuthReportedHttpException extends HTTPException { // NOSONAR, depth of class hierarchy OK
        private final String body;
        private final String msg;
        private OAuthReportedHttpException(int responseCode, String body) {
            super(responseCode);
        	msg = String.format("OAuth Response: %d%n", getStatusCode());
            this.body = body == null ? "" : body;
        }
        
        public String getErrorBody() {
            return body;
        }
        
        @Override
        public String getMessage() {
        	return msg + body;
        }
    }
    
    private URL getURL(String endpoint) throws MalformedURLException {
        if (isUsingQueryParameters()) {
        	endpoint = String.format("%s?username=%s&password=%s", 
        			endpoint,
        			URLEncoder.encode(username, StandardCharsets.UTF_8),
        			URLEncoder.encode(password, StandardCharsets.UTF_8)
        	);
        }
        return new URL(url, endpoint);
    	
    }
    
    private AccessToken getNewToken() throws IOException {
    	return requestAccessToken(this::writeAccessTokenRequest, getURL("oauth"), null);
    }
    
    private AccessToken refreshToken(AccessToken token) throws IOException {
        // If our refresh period expired, we need to get a brand new token.
        if (token == null || !token.isRefreshable()) {
            return getNewToken();
        }
    	return requestAccessToken(this::writeRefreshTokenRequest, getURL("oauth/refresh"), token);
    }

    private void writeAccessTokenRequest(HttpsURLConnection con, AccessToken notUsed) throws IOException {
        // Write the request content
    	// I believe we have to at least get the OutputStream or no Content-Length header will be written.
    	OutputStream os = con.getOutputStream();
        Writer w = new OutputStreamWriter(os, StandardCharsets.UTF_8);
    	if (!isUsingQueryParameters()) {
    	    w.write("username=");
            w.write(URLEncoder.encode(username, StandardCharsets.UTF_8));
            w.write("&password=");
            w.write(URLEncoder.encode(password, StandardCharsets.UTF_8));
            w.write("&grant_type=password");
    	}
    	w.flush();
    }

    private void writeRefreshTokenRequest(HttpsURLConnection con, AccessToken token) throws IOException {
    	OutputStream os = con.getOutputStream();
        Writer w = new OutputStreamWriter(os, StandardCharsets.UTF_8);
    	if (!isUsingQueryParameters()) {
            w.write("refresh_token=");
            w.write(URLEncoder.encode(token.getRefreshToken(), StandardCharsets.UTF_8));
            w.write("&grant_type=refresh_token");
    	}
    	w.flush();
    }

    private AccessToken requestAccessToken(RequestWriter requestWriter, URL base, AccessToken token) throws IOException {
        HttpsURLConnection con = (HttpsURLConnection) tlsSupport.getSNIEnabledConnection(base);
        prepareConnectionForFormPost(con);
        requestWriter.write(con, token);
        int responseCode = con.getResponseCode(); 
        if (responseCode == HttpServletResponse.SC_OK || responseCode == HttpServletResponse.SC_CREATED) {
            // Read the response back into a token.
            return save(readToken(con));
        }
        throw new OAuthReportedHttpException(responseCode, readError(con));
    }

	private String readError(HttpsURLConnection con) {
        try (InputStream is = con.getErrorStream()) {
        	if (is == null) {
        		return null;
        	}
          return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Get an existing token, or renew a previously existing token.
     * @return An unexpired token
     * @throws IOException  If an IO Error occured while getting a token. 
     */
    public String getToken() throws IOException  {
    	int retries = 0;
    	IOException lastEx = null;
    	
    	do {
    		try {
		        if (accessToken == null) {
		        	accessToken = getNewToken();
		        	log.info(Markers2.append(OAUTH_ENDPOINT, url.toString()), "Got new access token");
		        } else if (!accessToken.isValid()) {
		        	refreshOnFailure();
		        }
		        if (accessToken != null) {
		        	return accessToken.getAccessToken();
		        }
    		} catch (OAuthReportedHttpException ex) {
    			log.error(Markers2.append(ex, OAUTH_ENDPOINT, url.toString()), 
    					"Authorization failed for user {} to {}", username, url);
    			lastEx = new IOException("OAuth Exception: " + ex.getMessage(), ex);
    		} catch (IOException ex) {
    			log.error(Markers2.append(ex, OAUTH_ENDPOINT, url.toString()), 
    					"Exception on authorization for user {} to {}", username, url);
    			lastEx = ex;
    		} catch (Exception ex) {
    			log.error(Markers2.append(ex, OAUTH_ENDPOINT, url.toString()), 
    					"Exception on authorization for user {} to {}", username, url);
    			lastEx = new IOException("Unexpected exception: " + ex.getMessage(), ex);
    		}
    	} while (retries++ < numRetries);
    	
		accessToken = null;

		throw lastEx;
    }

	private void refreshOnFailure() throws IOException {
		try {
			accessToken = refreshToken(accessToken);
			log.info(Markers2.append(OAUTH_ENDPOINT, url.toString()), "Refreshed access token");
		} catch (OAuthReportedHttpException oex) {
			// On a refresh failure, try to get a brand new token instead.
			log.warn(Markers2.append(oex, OAUTH_ENDPOINT, url.toString()),
					"Authorization refresh failed for user {} to {}", username, url);
			accessToken = getNewToken();
			log.info(Markers2.append(OAUTH_ENDPOINT, url.toString()), 
					"Got new access token after failed refresh");
		}
	}

    private void prepareConnectionForFormPost(HttpsURLConnection con) throws java.net.ProtocolException {
        // Set up our trust store
    	SSLSocketFactory factory = con.getSSLSocketFactory();
    	if (isDebugging()) {
    		factory = new CapturingSSLSocketFactory(factory);
    	}
    	con.setSSLSocketFactory(factory);
        con.setUseCaches(false);
        // Write this out using application/x-www-form-urlencoded
        con.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED.toString());
        if (isUsingQueryParameters()) {
        	// Set Content-Length to 0 for query parameters.
        	con.setRequestProperty(HttpHeaders.CONTENT_LENGTH, "0");
        }
        // Set up POST request for read/write
        con.setDoInput(true);
        // No writing to this request if using query parameters.
        con.setDoOutput(true);
        con.setRequestMethod(HttpMethod.POST.toString());
    }

    private AccessToken readToken(HttpsURLConnection con) throws IOException {
        AccessToken token = null;
        try (InputStream is = con.getInputStream()) {
        	String value = IOUtils.toString(is, StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
            );
            token = mapper.readValue(new StringReader(value), AccessToken.class);
        }
        return token;
    }
}
