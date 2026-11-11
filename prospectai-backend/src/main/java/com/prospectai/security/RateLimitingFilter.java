package com.prospectai.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospectai.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter for AI-intensive endpoints.
 * <p>
 * Protected paths: {@code /api/chat/**} and {@code /api/analysis/**}.
 * The rate limit is per-IP (X-Forwarded-For header honoured for proxies).
 * When the limit is exceeded an HTTP 429 response is returned with a
 * machine-readable {@link ApiResponse} body so the frontend can surface a
 * meaningful error message rather than a blank failure.
 * </p>
 *
 * <p>Configuration properties:
 * <ul>
 *   <li>{@code app.rate-limit.max-requests} – requests allowed per window (default 60)</li>
 *   <li>{@code app.rate-limit.window-seconds} – sliding window in seconds (default 60)</li>
 * </ul>
 * </p>
 */
@Component
@Order(1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    /**
     * URL path prefixes subject to rate limiting.
     */
    private static final String[] RATE_LIMITED_PATHS = {
            "/api/chat/",
            "/api/analysis/"
    };

    /** Per-client timestamp log. Deque<Instant> is the sliding window bucket. */
    private final Map<String, Deque<Instant>> requestLog = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    @Value("${app.rate-limit.max-requests:60}")
    private int maxRequests;

    @Value("${app.rate-limit.window-seconds:60}")
    private long windowSeconds;

    public RateLimitingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String prefix : RATE_LIMITED_PATHS) {
            if (path.startsWith(prefix)) {
                return false; // apply filter
            }
        }
        return true; // skip filter for all other paths
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientKey = resolveClientKey(request);
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(windowSeconds);

        Deque<Instant> timestamps = requestLog.computeIfAbsent(clientKey, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Evict timestamps that have fallen outside the sliding window
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequests) {
                log.warn("Rate limit exceeded for client={}, path={}, requests={} in {}s",
                        clientKey, request.getRequestURI(), timestamps.size(), windowSeconds);
                sendRateLimitResponse(response);
                return;
            }

            timestamps.addLast(now);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolves a stable identifier for the requesting client.
     * Prefers the first IP in {@code X-Forwarded-For} (set by load balancers)
     * and falls back to {@code request.getRemoteAddr()}.
     */
    private String resolveClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(windowSeconds));

        ApiResponse<Void> body = ApiResponse.error(
                "Rate limit exceeded. You may send at most " + maxRequests +
                " AI requests per " + windowSeconds + " seconds. Please wait and try again."
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
