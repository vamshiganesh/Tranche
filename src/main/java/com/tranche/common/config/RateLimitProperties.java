package com.tranche.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "tranche.rate-limit.commitment")
public record RateLimitProperties(
        boolean enabled,
        int requestsPerWindow,
        Duration window
) {
    public RateLimitProperties {
        if (requestsPerWindow <= 0) {
            requestsPerWindow = 10;
        }
        if (window == null || window.isZero() || window.isNegative()) {
            window = Duration.ofMinutes(1);
        }
    }
}
