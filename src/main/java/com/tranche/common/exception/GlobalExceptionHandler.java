package com.tranche.common.exception;

import com.tranche.common.util.CorrelationIdFilter;
import com.tranche.common.util.CorrelationIdHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        HttpStatus status = mapStatus(ex.getCode());
        return buildResponse(status, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Authentication required");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        if (CorrelationIdFilter.CORRELATION_ID_HEADER.equalsIgnoreCase(ex.getHeaderName())) {
            return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Missing required header");
        }
        if ("Idempotency-Key".equalsIgnoreCase(ex.getHeaderName())) {
            return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.MISSING_IDEMPOTENCY_KEY, "Idempotency-Key header is required");
        }
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Missing required header: " + ex.getHeaderName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Malformed or unreadable request body");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value for parameter '" + ex.getName() + "'";
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String message = "HTTP method " + ex.getMethod() + " is not supported for this endpoint";
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error [correlationId={}]", CorrelationIdHolder.get(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, ErrorCode code, String message) {
        ApiError error = new ApiError(
                code.name(),
                message,
                CorrelationIdHolder.get(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(new ErrorResponse(error));
    }

    private HttpStatus mapStatus(ErrorCode code) {
        return switch (code) {
            case VALIDATION_ERROR, OPPORTUNITY_NOT_EDITABLE, MISSING_IDEMPOTENCY_KEY,
                    INVALID_VERIFICATION_CODE -> HttpStatus.BAD_REQUEST;
            case INVALID_CREDENTIALS, UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, INVALID_ROLE, EMAIL_NOT_VERIFIED, DEMO_CREDIT_NOT_AVAILABLE -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT, PROFILE_ALREADY_EXISTS, INVALID_STATE_TRANSITION -> HttpStatus.CONFLICT;
            case OPPORTUNITY_NOT_LIVE, INSUFFICIENT_UNITS, INSUFFICIENT_FUNDS, BELOW_MINIMUM_LOT,
                    KYC_NOT_APPROVED, KYB_NOT_APPROVED -> HttpStatus.UNPROCESSABLE_ENTITY;
            case RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
