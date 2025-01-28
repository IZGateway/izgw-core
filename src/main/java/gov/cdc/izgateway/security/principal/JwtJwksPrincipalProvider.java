package gov.cdc.izgateway.security.principal;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtJwksPrincipalProvider extends AbstractJwtPrincipalProvider {
    @Value("${jwt.jwk-set-uri:}")
    private String jwkSetUri;

    public JwtJwksPrincipalProvider(@Nullable GroupToRoleMapper groupToRoleMapper,
                                    @Nullable ScopeToRoleMapper scopeToRoleMapper,
                                    JwtTokenExtractor jwtTokenExtractor) {
        super(groupToRoleMapper, scopeToRoleMapper, jwtTokenExtractor);
    }

    @Override
    protected NimbusJwtDecoder createDecoder() {
        return NimbusJwtDecoder
                .withJwkSetUri(jwkSetUri)
                .build();
    }

    @Override
    protected boolean validConfiguration() {
        if (StringUtils.isNotBlank(jwkSetUri)) {
            return true;
        }

        log.warn("No JWT set URI configured.  JWT authentication is disabled.");
        return false;
    }
}
