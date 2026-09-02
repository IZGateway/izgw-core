package gov.cdc.izgateway.security.principal;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class JwtTokenExtractor {

    private static final Pattern JWS_COMPACT =
            Pattern.compile("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");

    public String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    		log.trace("No JWT token found in Authorization header");
            throw new MissingJwtTokenException("No valid JWT token in Authorization header");
        }
        String token = authHeader.substring(7).trim();
        if (token.isEmpty() || !isJwtFormat(token)) {
    		log.trace("No JWT token found in Authorization header");
            throw new MissingJwtTokenException("No valid JWT token in Authorization header");
        }
        return token;
    }

    private static boolean isJwtFormat(String token) {
        return token != null && JWS_COMPACT.matcher(token).matches();
    }
}
