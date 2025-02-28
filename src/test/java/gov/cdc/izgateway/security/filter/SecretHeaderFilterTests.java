// File: src/test/java/gov/cdc/izgateway/security/filter/SecretHeaderFilterTests.java

package gov.cdc.izgateway.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecretHeaderFilterTests {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private SecretHeaderFilter filter;

    @BeforeEach
    void setUp() {
        // Initialize the filter with test values
        filter = new SecretHeaderFilter(true, "x-alb-secret", "secret-value");
    }

    @Test
    void testFilterDisabled() throws IOException, ServletException {
        // Initialize the filter with disabled state
        filter = new SecretHeaderFilter(false, "", "");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void testFilterEnabledWithoutKeyOrValue() {
        // Expect IllegalStateException when filter is enabled but key or value is missing
        assertThrows(IllegalStateException.class, () -> new SecretHeaderFilter(true, "", ""));
    }

    @Test
    void testRequestWithoutHeader() throws IOException, ServletException {
        when(request.getHeader("x-alb-secret")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void testRequestWithInvalidHeader() throws IOException, ServletException {
        when(request.getHeader("x-alb-secret")).thenReturn("invalid-value");

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void testRequestWithValidHeader() throws IOException, ServletException {
        when(request.getHeader("x-alb-secret")).thenReturn("secret-value");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }
}
