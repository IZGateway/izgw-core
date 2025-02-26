package gov.cdc.izgateway.security.principal;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenExtractor {
    public String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No JWT token found in Authorization header");
            throw new InvalidJwtTokenException("No valid JWT token in Authorization header");
        }
        return authHeader.substring(7);
    }
}
