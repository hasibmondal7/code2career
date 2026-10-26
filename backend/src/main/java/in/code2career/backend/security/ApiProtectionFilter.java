package in.code2career.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ApiProtectionFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000L;

    private final ConcurrentHashMap<String, Deque<Long>> requests = new ConcurrentHashMap<>();
    private final int maxRequestBytes;
    private final int generalRequestsPerMinute;
    private final int authRequestsPerMinute;
    private final int executionRequestsPerMinute;

    public ApiProtectionFilter(
            @Value("${app.api.max-request-bytes:16777216}") int maxRequestBytes,
            @Value("${app.api.general-requests-per-minute:120}") int generalRequestsPerMinute,
            @Value("${app.api.auth-requests-per-minute:10}") int authRequestsPerMinute,
            @Value("${app.api.execution-requests-per-minute:20}") int executionRequestsPerMinute
    ) {
        this.maxRequestBytes = maxRequestBytes;
        this.generalRequestsPerMinute = generalRequestsPerMinute;
        this.authRequestsPerMinute = authRequestsPerMinute;
        this.executionRequestsPerMinute = executionRequestsPerMinute;
        if (maxRequestBytes < 1 || maxRequestBytes == Integer.MAX_VALUE
                || generalRequestsPerMinute < 1 || authRequestsPerMinute < 1 || executionRequestsPerMinute < 1) {
            throw new IllegalArgumentException("Request limits must be positive and maximum request size must be below Integer.MAX_VALUE");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/")
                || "/api/health".equals(path)
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (request.getContentLengthLong() > maxRequestBytes) {
            writeError(response, HttpStatus.CONTENT_TOO_LARGE, "Request body is too large");
            return;
        }
        String path = request.getRequestURI();
        String bucket = bucketFor(path);
        int limit = limitFor(bucket);
        String key = request.getRemoteAddr() + ':' + bucket;
        if (!allowRequest(key, limit)) {
            response.setHeader("Retry-After", "60");
            writeError(response, HttpStatus.TOO_MANY_REQUESTS, "Too many requests");
            return;
        }

        if (request.getContentLengthLong() < 0) {
            byte[] body = request.getInputStream().readNBytes(maxRequestBytes + 1);
            if (body.length > maxRequestBytes) {
                writeError(response, HttpStatus.CONTENT_TOO_LARGE, "Request body is too large");
                return;
            }
            request = new CachedBodyHttpServletRequest(request, body);
        }

        filterChain.doFilter(request, response);
    }

    private int limitFor(String bucket) {
        return switch (bucket) {
            case "auth" -> authRequestsPerMinute;
            case "execution" -> executionRequestsPerMinute;
            default -> generalRequestsPerMinute;
        };
    }

    private String bucketFor(String path) {
        if (path.endsWith("/login") || path.endsWith("/register")
                || path.endsWith("/password-reset/request")
                || path.endsWith("/password-reset/confirm")) {
            return "auth";
        }
        return path.startsWith("/api/submissions") ? "execution" : "general";
    }

    private boolean allowRequest(String key, int limit) {
        long now = System.nanoTime() / 1_000_000L;
        if (requests.size() > 10_000) {
            requests.entrySet().removeIf(entry -> {
                Deque<Long> timestamps = entry.getValue();
                synchronized (timestamps) {
                    return timestamps.isEmpty()
                            || now - timestamps.peekLast() >= WINDOW_MILLIS;
                }
            });
        }
        Deque<Long> timestamps = requests.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= WINDOW_MILLIS) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= limit) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":" + status.value() + ",\"error\":\"" + message + "\"}");
    }
}
