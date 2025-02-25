package gov.cdc.izgateway.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IpAddressFilterTests {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        reset(request, response, filterChain);
    }

    @Test
    void testConstructorWithValidCidr() {
        IpAddressFilter filter = new IpAddressFilter("192.168.1.0/24,10.0.0.0/8", true);
        assertNotNull(filter);
    }

    @Test
    void testConstructorWithEmptyCidrWhenEnabled() {
        assertThrows(IllegalStateException.class, () -> new IpAddressFilter("", true));
    }

    @Test
    void testConstructorWithNullCidrWhenEnabled() {
        assertThrows(IllegalStateException.class, () -> new IpAddressFilter(null, true));
    }

    @Test
    void testConstructorWithEmptyCidrWhenDisabled() {
        IpAddressFilter filter = new IpAddressFilter("", false);
        assertNotNull(filter);
    }

    @Test
    void testAllowedIpWhenFilteringEnabled() throws IOException, ServletException {
        IpAddressFilter filter = new IpAddressFilter("192.168.1.0/24", true);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void testDisallowedIpWhenFilteringEnabled() throws IOException, ServletException {
        IpAddressFilter filter = new IpAddressFilter("192.168.1.0/24", true);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void testAnyIpAllowedWhenFilteringDisabled() throws IOException, ServletException {
        IpAddressFilter filter = new IpAddressFilter("192.168.1.0/24", false);
        // IP outside the allowed range, but should not matter as filtering is disabled
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void testMultipleCidrRanges() throws IOException, ServletException {
        IpAddressFilter filter = new IpAddressFilter("192.168.1.0/24,10.0.0.0/8", true);

        // Test first range
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        filter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        reset(request, response, filterChain);

        // Test second range
        when(request.getRemoteAddr()).thenReturn("10.10.10.10");
        filter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        reset(request, response, filterChain);

        // Test outside range
        when(request.getRemoteAddr()).thenReturn("172.16.0.1");
        filter.doFilter(request, response, filterChain);
        verify(filterChain, never()).doFilter(any(), any());
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void testIpv6Localhost() throws IOException, ServletException {
        IpAddressFilter filter = new IpAddressFilter("127.0.0.1/32,::1/128", true);

        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");
        filter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());

        reset(request, response, filterChain);
    }

    @Test
    void testNonLocalIpv6Address() throws IOException, ServletException {
        // Setup with IPv4 CIDR only
        IpAddressFilter filter = new IpAddressFilter("::1/128", true);

        // Test with a non-localhost IPv6 address
        when(request.getRemoteAddr()).thenReturn("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        filter.doFilter(request, response, filterChain);

        // Should be denied as the IPv6 address isn't in allowed CIDRs
        verify(filterChain, never()).doFilter(any(), any());
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void testAllowedIpv6Address() throws IOException, ServletException {
        // Setup with both IPv4 and IPv6 CIDRs
        IpAddressFilter filter = new IpAddressFilter("2001:0db8:85a3:0000:0000:8a2e:0370:7334/128", true);

        // Test with an allowed IPv6 address
        when(request.getRemoteAddr()).thenReturn("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        filter.doFilter(request, response, filterChain);

        // Should be allowed as the IPv6 address is in allowed CIDR
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }
}
