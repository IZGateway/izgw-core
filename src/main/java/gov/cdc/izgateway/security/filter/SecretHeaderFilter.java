package gov.cdc.izgateway.security.filter;

import gov.cdc.izgateway.logging.RequestContext;
import gov.cdc.izgateway.logging.markers.Markers2;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import gov.cdc.izgateway.security.AccessControlValve;

import java.io.IOException;
import java.util.List;

/**
 * SecretHeaderFilter is a servlet filter that checks for a specific header in incoming requests.
 * This is used to ensure that only requests from internal services (like ALB or WAF) are allowed to pass through.
 * It is disabled by default, but can be enabled via the settings in the constructor.
 * If the filter is enabled but the key or value is missing, an IllegalStateException is thrown.
 */
@Slf4j
@Component
@Order()
public class SecretHeaderFilter implements Filter {
    private final boolean headerFilterEnabled;
    private final String headerFilterKey;
    private final String headerFilterValue;
    private final List<String> bypassPaths;

    public SecretHeaderFilter(
            @Value("${hub.security.secret-header-filter.enabled:false}") boolean headerFilterEnabled,
            @Value("${hub.security.secret-header-filter.key:}") String headerFilterKey,
            @Value("${hub.security.secret-header-filter.value:}") String headerFilterValue,
            @Value("${hub.security.secret-header-filter.bypass-paths:/rest/health}") List<String> bypassPaths
    ) {
        this.headerFilterEnabled = headerFilterEnabled;
        this.headerFilterKey = headerFilterKey;
        this.headerFilterValue = headerFilterValue;
        this.bypassPaths = bypassPaths;

        if (this.headerFilterEnabled) {
            if (StringUtils.isEmpty(this.headerFilterKey) || StringUtils.isEmpty(this.headerFilterValue)) {
                throw new IllegalStateException("Secret header filter is enabled, but the header key or value is not set.");
            }
        } else {
            log.warn("Secret header filter not enabled. Requests will not be filtered.");
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (bypassThisFilter(servletRequest)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // Check if the request is from an internal service (ALB, WAF) by checking the header
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String headerValue = request.getHeader(headerFilterKey);

        if (headerValue == null || !headerValue.equals(headerFilterValue)) {
            log.error(Markers2.append(RequestContext.getSourceInfo()), "Request does not contain the secret header, rejecting request.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Continue the filter chain
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private boolean bypassThisFilter(ServletRequest servletRequest) {
        if (!headerFilterEnabled || AccessControlValve.isLocalHost(servletRequest.getRemoteAddr())) {
            return true;
        }

        String requestURI = ((HttpServletRequest) servletRequest).getRequestURI();
        return bypassPaths.stream().anyMatch(requestURI::equalsIgnoreCase);
    }
}
