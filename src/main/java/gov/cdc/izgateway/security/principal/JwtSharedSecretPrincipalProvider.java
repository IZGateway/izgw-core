package gov.cdc.izgateway.security.principal;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.ServiceConfigurationError;

import javax.crypto.spec.SecretKeySpec;

/**
 * Principal provider that validates JWTs using a shared secret.
 * @author Audacious Inquiry
 */
@Slf4j
@Component
public class JwtSharedSecretPrincipalProvider extends AbstractJwtPrincipalProvider {
    private String sharedSecret;
    Boolean isValid = null;

    /**
	 * Constructor 
	 * @param groupToRoleMapper The group to role mapper
	 * @param scopeToRoleMapper The scope to role mapper
	 * @param jwtTokenExtractor The JWT token extractor
	 * @param sharedSecret The shared secret for validating JWTs (base64-encoded)
	 */
    @Autowired
    public JwtSharedSecretPrincipalProvider(@Nullable GroupToRoleMapper groupToRoleMapper,
                                            @Nullable ScopeToRoleMapper scopeToRoleMapper,
                                            JwtTokenExtractor jwtTokenExtractor,     
                                            @Value("${jwt.shared-secret:}") String sharedSecret
    ) {
        super(groupToRoleMapper, scopeToRoleMapper, jwtTokenExtractor);
    	this.sharedSecret = sharedSecret;
    }

    @Override
    protected NimbusJwtDecoder createDecoder() {
    	try {
	    	SecretKeySpec secretKey = new SecretKeySpec(Base64.getDecoder().decode(sharedSecret), "HmacSHA256");
	        return NimbusJwtDecoder
	                .withSecretKey(secretKey)
	                .build();
    	} catch (IllegalArgumentException ex) {
    		throw new ServiceConfigurationError("Failed to decode JWT shared secret. Ensure it is base64-encoded.", ex);
    	}
    }

    @Override
    protected boolean validConfiguration() {
    	if (isValid != null) {
			return isValid;
		}
    	try {
	        if (StringUtils.isNotBlank(sharedSecret) && Base64.getDecoder().decode(sharedSecret) != null) {
	            return isValid = true;
	        }
    	} catch (IllegalArgumentException ex) {
    		log.warn("Invalid JWT shared secret was set, ensure it is base64 encoded. JWT authentication is disabled.");// Ignore - will log below
            return isValid = false;
    	}
        log.warn("No JWT shared secret was set. JWT authentication is disabled.");
        return isValid = false;
    }
}
