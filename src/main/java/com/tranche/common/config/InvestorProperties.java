package com.tranche.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "tranche.investor")
public record InvestorProperties(
        BigDecimal defaultWalletBalance,
        String defaultCurrency,
        Boolean demoCreditEnabled,
        BigDecimal demoCreditAmount
) {
    public InvestorProperties {
        if (defaultWalletBalance == null) {
            defaultWalletBalance = BigDecimal.ZERO;
        }
        if (defaultCurrency == null) {
            defaultCurrency = "USD";
        }
        if (demoCreditEnabled == null) {
            demoCreditEnabled = false;
        }
        if (demoCreditAmount == null) {
            demoCreditAmount = new BigDecimal("3000000");
        }
    }
}
