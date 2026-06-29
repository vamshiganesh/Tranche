package com.tranche.common.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs request method, path, status, and duration. Correlation ID is propagated via MDC for log aggregation.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String MDC_CORRELATION_ID = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String correlationId = CorrelationIdHolder.get();
        if (correlationId != null) {
            MDC.put(MDC_CORRELATION_ID, correlationId);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (log.isInfoEnabled() && !isHealthCheck(request)) {
                long durationMs = System.currentTimeMillis() - start;
                log.info("{} {} {} {}ms",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        durationMs);
            }
            MDC.remove(MDC_CORRELATION_ID);
        }
    }

    private static boolean isHealthCheck(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health");
    }
}
