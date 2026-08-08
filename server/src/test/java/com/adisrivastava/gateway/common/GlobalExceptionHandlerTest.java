package com.adisrivastava.gateway.common;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void gatewayExceptionProducesTheErrorEnvelope() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "req_test123");

        try {
            GatewayException ex = new GatewayException(
                    HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT", "rate_limit_exceeded",
                    "Rate limit of 60 requests/min exceeded");

            ResponseEntity<ErrorResponse> response = handler.handleGateway(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

            ErrorResponse.ErrorDetail detail = response.getBody().error();
            assertThat(detail.type()).isEqualTo("rate_limit_exceeded");
            assertThat(detail.code()).isEqualTo("RATE_LIMIT");
            assertThat(detail.requestId()).isEqualTo("req_test123");
            assertThat(detail.message()).contains("60 requests/min");
        } finally {
            MDC.clear();
        }
    }
}
