package com.tranche.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tranche.common.exception.ApiError;
import com.tranche.common.exception.ErrorCode;
import com.tranche.common.exception.ErrorResponse;
import com.tranche.common.util.CorrelationIdHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = new ErrorResponse(new ApiError(
                ErrorCode.UNAUTHORIZED.name(),
                "Authentication required",
                CorrelationIdHolder.get(),
                Instant.now()
        ));
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
