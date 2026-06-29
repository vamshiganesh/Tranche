package com.tranche.allocation.domain;

import com.tranche.common.exception.BusinessException;
import com.tranche.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllocationCalculatorTest {

    private static final BigDecimal UNIT_PRICE = new BigDecimal("10000.0000");
    private static final BigDecimal MINIMUM_LOT = new BigDecimal("10000.0000");

    @Test
    void fullFillWhenEnoughUnits() {
        var decision = AllocationCalculator.computeFill(10, 100, UNIT_PRICE, MINIMUM_LOT);

        assertThat(decision.rejected()).isFalse();
        assertThat(decision.fillStatus()).isEqualTo(FillStatus.FULL);
        assertThat(decision.unitsAllocated()).isEqualTo(10);
        assertThat(decision.amountAllocated()).isEqualByComparingTo("100000.0000");
    }

    @Test
    void partialFillWhenRequestExceedsRemaining() {
        var decision = AllocationCalculator.computeFill(50, 30, UNIT_PRICE, MINIMUM_LOT);

        assertThat(decision.rejected()).isFalse();
        assertThat(decision.fillStatus()).isEqualTo(FillStatus.PARTIAL);
        assertThat(decision.unitsAllocated()).isEqualTo(30);
        assertThat(decision.amountAllocated()).isEqualByComparingTo("300000.0000");
    }

    @Test
    void rejectsWhenNoUnitsRemain() {
        var decision = AllocationCalculator.computeFill(5, 0, UNIT_PRICE, MINIMUM_LOT);

        assertThat(decision.rejected()).isTrue();
        assertThat(decision.rejectionCode()).isEqualTo(ErrorCode.INSUFFICIENT_UNITS);
    }

    @Test
    void rejectsPartialBelowMinimumLot() {
        BigDecimal smallUnitPrice = new BigDecimal("5000.0000");
        var decision = AllocationCalculator.computeFill(10, 1, smallUnitPrice, MINIMUM_LOT);

        assertThat(decision.rejected()).isTrue();
        assertThat(decision.rejectionCode()).isEqualTo(ErrorCode.BELOW_MINIMUM_LOT);
    }

    @Test
    void validatesRequestedAmount() {
        assertThatThrownBy(() -> AllocationCalculator.validateRequestedAmount(
                10,
                UNIT_PRICE,
                new BigDecimal("99999.0000")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void computesYieldFromFaceValue() {
        var yield = AllocationCalculator.computeYield(
                new BigDecimal("1000000.0000"),
                100,
                10,
                new BigDecimal("100000.0000")
        );

        assertThat(yield.expectedReturn()).isEqualByComparingTo("100000.0000");
        assertThat(yield.discountAmount()).isEqualByComparingTo("0.0000");
    }
}
