package com.tranche.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tranche.common.exception.ApiError;
import com.tranche.common.exception.ErrorCode;
import com.tranche.common.exception.ErrorResponse;
import com.tranche.common.security.UserPrincipal;
import com.tranche.common.util.CorrelationIdHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Applies per-investor rate limits to POST /api/v1/opportunities/{id}/commitments.
 * Runs after JWT authentication so the principal is available.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class CommitmentRateLimitFilter extends OncePerRequestFilter {

    private static final String COMMITMENT_PATH_PATTERN = ".*/api/v1/opportunities/\\d+/commitments/?$";

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public CommitmentRateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().matches(COMMITMENT_PATH_PATTERN);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Long investorId = resolveInvestorId();
        if (investorId != null && !rateLimitService.tryAcquireCommitment(investorId)) {
            writeRateLimitResponse(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Long resolveInvestorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        ApiError error = new ApiError(
                ErrorCode.RATE_LIMIT_EXCEEDED.name(),
                "Commitment rate limit exceeded; try again later",
                CorrelationIdHolder.get(),
                Instant.now()
        );
        response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(error));
    }
}
