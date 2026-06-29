package com.tranche.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tranche.pagination")
public record PaginationProperties(
        int defaultPageSize,
        int maxPageSize
) {
    public PaginationProperties {
        if (defaultPageSize <= 0) {
            defaultPageSize = 20;
        }
        if (maxPageSize <= 0) {
            maxPageSize = 100;
        }
        if (defaultPageSize > maxPageSize) {
            defaultPageSize = maxPageSize;
        }
    }
}
