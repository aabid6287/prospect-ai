package com.prospectai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that attaches a correlation ID to every inbound request.
 * <p>
 * The ID is sourced from the {@code X-Correlation-ID} request header when
 * provided by the caller, otherwise a random UUID is generated.
 * The ID is stored in SLF4J MDC under {@code correlationId} so that it
 * appears in every log statement for the duration of the request, and is
 * echoed back in the {@code X-Correlation-ID} response header for
 * end-to-end tracing.
 * </p>
 * <p>
 * Registered at {@code Ordered.HIGHEST_PRECEDENCE} so that the ID is present
 * before any Spring Security or application filter logs a message.
 * </p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Put into MDC for structured logging; clear in finally to avoid leaks
        MDC.put(MDC_KEY, correlationId);
        // Propagate to caller so distributed traces can be correlated
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
