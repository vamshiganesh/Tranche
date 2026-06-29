package com.tranche.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tranche.outbox")
public record OutboxProperties(
        boolean pollingEnabled,
        long pollIntervalMs,
        int batchSize
) {
    public OutboxProperties {
        if (pollIntervalMs <= 0) {
            pollIntervalMs = 30_000L;
        }
        if (batchSize <= 0) {
            batchSize = 50;
        }
    }
}
