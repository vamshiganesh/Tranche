package com.tranche.common.exception;

import com.tranche.common.util.CorrelationIdHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        CorrelationIdHolder.set("test-correlation-id");
    }

    @AfterEach
    void tearDown() {
        CorrelationIdHolder.clear();
    }

    @Test
    void mapsBusinessNotFoundTo404() {
        var response = handler.handleBusiness(new ResourceNotFoundException("Opportunity not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error().code()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().error().correlationId()).isEqualTo("test-correlation-id");
    }

    @Test
    void mapsMissingIdempotencyHeaderTo400() {
        var response = handler.handleMissingHeader(
                new MissingRequestHeaderException("Idempotency-Key", null)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().code()).isEqualTo("MISSING_IDEMPOTENCY_KEY");
    }

    @Test
    void hidesInternalExceptionDetails() {
        var response = handler.handleUnexpected(new RuntimeException("database password leaked"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().message()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().error().message()).doesNotContain("password");
    }

    @Test
    void mapsUnreadableJsonToValidationError() {
        var response = handler.handleUnreadableBody(
                new HttpMessageNotReadableException("bad json")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().code()).isEqualTo("VALIDATION_ERROR");
    }
}
