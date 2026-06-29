package com.tranche.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "tranche.investor")
public record InvestorProperties(
        BigDecimal defaultWalletBalance,
        String defaultCurrency
) {
    public InvestorProperties {
        if (defaultWalletBalance == null) {
            defaultWalletBalance = BigDecimal.ZERO;
        }
        if (defaultCurrency == null) {
            defaultCurrency = "USD";
        }
    }
}
