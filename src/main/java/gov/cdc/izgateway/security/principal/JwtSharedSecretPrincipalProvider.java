package gov.cdc.izgateway.security.principal;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

@Slf4j
@Component
public class JwtSharedSecretPrincipalProvider extends AbstractJwtPrincipalProvider {
    @Value("${jwt.shared-secret:}")
    private String sharedSecret;

    @Autowired
    public JwtSharedSecretPrincipalProvider(@Nullable GroupToRoleMapper groupToRoleMapper,
                                            @Nullable ScopeToRoleMapper scopeToRoleMapper,
                                            JwtTokenExtractor jwtTokenExtractor) {
        super(groupToRoleMapper, scopeToRoleMapper, jwtTokenExtractor);
    }

    @Override
    protected NimbusJwtDecoder createDecoder() {
        SecretKeySpec secretKey = new SecretKeySpec(sharedSecret.getBytes(), "HmacSHA256");
        return NimbusJwtDecoder
                .withSecretKey(secretKey)
                .build();
    }

    @Override
    protected boolean validConfiguration() {
        if (StringUtils.isNotBlank(sharedSecret)) {
            return true;
        }

        log.warn("No JWT shared secret was set. JWT authentication is disabled.");
        return false;
    }
}
