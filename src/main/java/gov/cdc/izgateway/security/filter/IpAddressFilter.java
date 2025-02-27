package gov.cdc.izgateway.security.filter;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IpAddressFilter implements Filter {
    private List<IPAddress> allowedSubnets = Collections.emptyList();
    private final boolean ipFilterEnabled;

    public IpAddressFilter(
            @Value("${hub.security.ip-filter.allowed-cidr:}") String allowedCidr,
            @Value("${hub.security.ip-filter.enabled:false}") boolean ipFilterEnabled
    ) {
        this.ipFilterEnabled = ipFilterEnabled;

        if (this.ipFilterEnabled) {
            if (allowedCidr == null || allowedCidr.trim().isEmpty()) {
                throw new IllegalStateException("IP filtering enabled, no IP CIDRs configured.");
            }

            this.allowedSubnets = Arrays.stream(allowedCidr.split(","))
                    .map(String::trim)
                    .filter(cidr -> !cidr.isEmpty())
                    .map(cidr -> new IPAddressString(cidr).getAddress())
                    .collect(Collectors.toList());

            log.info("IP whitelist configured with {} CIDR blocks: {}", this.allowedSubnets.size(), allowedCidr);

        } else {
            log.warn("IP filtering not enabled. All IP addresses will be allowed.");
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;

        // Getting the remote address, not considering X-Forwarded-For currently
        // This gets the IP from the ALB itself whereas X-Forwarded-For would get
        // IP address of actual caller.
        String clientIp = httpRequest.getRemoteAddr();

        if (!ipFilterEnabled) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        boolean allowed;
        try {
            IPAddress ipAddress = new IPAddressString(clientIp).getAddress();
            allowed = allowedSubnets.stream().anyMatch(subnet -> subnet.contains(ipAddress));

        } catch (Exception e) {
            // We were unable to parse the IP address, block access
            log.warn("Unable to parse/verify IP: {}. Error: {}", clientIp, e.getMessage());
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (allowed) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            log.warn("Access denied for IP: {}. Not in any configured allowed CIDR.", clientIp);
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
