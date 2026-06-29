package com.tranche.portfolio.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RealizedYieldCalculatorTest {

    @Test
    void computesAnnualizedYield() {
        BigDecimal yield = RealizedYieldCalculator.annualizedPercent(
                new BigDecimal("100000.0000"),
                new BigDecimal("108500.0000"),
                90
        );

        assertThat(yield).isEqualByComparingTo("34.4700");
    }
}
