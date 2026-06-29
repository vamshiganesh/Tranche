package com.tranche.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tranche.jwt")
public record JwtProperties(
        String secret,
        long expirationMs
) {
}
