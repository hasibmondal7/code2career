package in.code2career.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiProtectionFilterTest {

    @Test
    void rejectsUnknownLengthBodyWhenItExceedsConfiguredLimit() throws Exception {
        ApiProtectionFilter filter = new ApiProtectionFilter(4, 120, 10, 20);
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/users/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getContentLengthLong()).thenReturn(-1L);
        when(request.getInputStream()).thenReturn(inputStream("12345"));

        filter.doFilterInternal(request, response, chain);

        assertEquals(413, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void replaysUnknownLengthBodyWithinLimitToTheApplication() throws Exception {
        ApiProtectionFilter filter = new ApiProtectionFilter(4, 120, 10, 20);
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        var response = new MockHttpServletResponse();
        when(request.getRequestURI()).thenReturn("/api/users/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getContentLengthLong()).thenReturn(-1L);
        when(request.getInputStream()).thenReturn(inputStream("1234"));

        filter.doFilterInternal(request, response, (wrappedRequest, ignoredResponse) -> {
            byte[] body = wrappedRequest.getInputStream().readAllBytes();
            assertEquals("1234", new String(body, StandardCharsets.UTF_8));
        });

        assertEquals(200, response.getStatus());
    }

    @Test
    void sharesAuthenticationRateLimitAcrossEndpointsForSameClient() throws Exception {
        ApiProtectionFilter filter = new ApiProtectionFilter(1024, 1, 1, 20);
        var firstRequest = request("/api/users/login");
        var secondRequest = request("/api/users/password-reset/request");
        var firstResponse = new MockHttpServletResponse();
        var secondResponse = new MockHttpServletResponse();
        FilterChain firstChain = mock(FilterChain.class);
        FilterChain secondChain = mock(FilterChain.class);

        filter.doFilterInternal(firstRequest, firstResponse, firstChain);
        filter.doFilterInternal(secondRequest, secondResponse, secondChain);

        assertEquals(200, firstResponse.getStatus());
        assertEquals(429, secondResponse.getStatus());
        verify(firstChain).doFilter(firstRequest, firstResponse);
        verify(secondChain, never()).doFilter(secondRequest, secondResponse);
    }

    private jakarta.servlet.http.HttpServletRequest request(String path) {
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(path);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getContentLengthLong()).thenReturn(0L);
        return request;
    }

    private ServletInputStream inputStream(String content) {
        ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        return new ServletInputStream() {
            @Override
            public int read() {
                return input.read();
            }

            @Override
            public boolean isFinished() {
                return input.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(jakarta.servlet.ReadListener readListener) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
