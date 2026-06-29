package com.tranche.portfolio.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Annualized realized yield from invested principal and expected return over a fixed tenure.
 */
public final class RealizedYieldCalculator {

    private static final int RATE_SCALE = 4;

    private RealizedYieldCalculator() {
    }

    public static BigDecimal annualizedPercent(
            BigDecimal investedAmount,
            BigDecimal expectedReturn,
            int tenureDays
    ) {
        if (tenureDays <= 0 || investedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal profit = expectedReturn.subtract(investedAmount);
        return profit
                .divide(investedAmount, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(365))
                .divide(BigDecimal.valueOf(tenureDays), RATE_SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
