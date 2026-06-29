package com.tranche.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "tranche.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
}
