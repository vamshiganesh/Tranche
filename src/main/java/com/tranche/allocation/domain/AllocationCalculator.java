package com.tranche.allocation.domain;

import com.tranche.common.exception.BusinessException;
import com.tranche.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure allocation math — no locking, no I/O. Used by {@link com.tranche.allocation.service.AllocationEngine}
 * after the opportunity row is locked so remaining units are read from a single authoritative source.
 */
public final class AllocationCalculator {

    private static final int MONEY_SCALE = 4;

    private AllocationCalculator() {
    }

    public record FillDecision(
            int unitsAllocated,
            BigDecimal amountAllocated,
            FillStatus fillStatus,
            boolean rejected,
            ErrorCode rejectionCode,
            String rejectionMessage
    ) {
        public static FillDecision rejected(ErrorCode code, String message) {
            return new FillDecision(0, BigDecimal.ZERO, FillStatus.REJECTED, true, code, message);
        }

        public static FillDecision confirmed(int unitsAllocated, BigDecimal amountAllocated, FillStatus fillStatus) {
            return new FillDecision(unitsAllocated, amountAllocated, fillStatus, false, null, null);
        }
    }

    public record YieldSnapshot(BigDecimal discountAmount, BigDecimal expectedReturn) {
    }

    public static FillDecision computeFill(
            int unitsRequested,
            int remainingUnits,
            BigDecimal unitPrice,
            BigDecimal minimumLot
    ) {
        if (remainingUnits <= 0) {
            return FillDecision.rejected(
                    ErrorCode.INSUFFICIENT_UNITS,
                    "No units available for allocation"
            );
        }

        int unitsToAllocate = Math.min(unitsRequested, remainingUnits);
        BigDecimal amountAllocated = unitPrice.multiply(BigDecimal.valueOf(unitsToAllocate))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        if (amountAllocated.compareTo(minimumLot) < 0) {
            return FillDecision.rejected(
                    ErrorCode.BELOW_MINIMUM_LOT,
                    "Allocated amount is below the minimum lot size"
            );
        }

        FillStatus fillStatus = unitsToAllocate < unitsRequested ? FillStatus.PARTIAL : FillStatus.FULL;
        return FillDecision.confirmed(unitsToAllocate, amountAllocated, fillStatus);
    }

    public static void validateRequestedAmount(int unitsRequested, BigDecimal unitPrice, BigDecimal amountRequested) {
        BigDecimal expected = unitPrice.multiply(BigDecimal.valueOf(unitsRequested))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (expected.compareTo(amountRequested.setScale(MONEY_SCALE, RoundingMode.HALF_UP)) != 0) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "amount must equal unitsRequested × unitPrice"
            );
        }
    }

    public static void validateMinimumLot(BigDecimal amountRequested, BigDecimal minimumLot) {
        if (amountRequested.compareTo(minimumLot) < 0) {
            throw new BusinessException(
                    ErrorCode.BELOW_MINIMUM_LOT,
                    "Requested amount is below the minimum lot size"
            );
        }
    }

    public static YieldSnapshot computeYield(
            BigDecimal faceValue,
            int totalUnits,
            int unitsAllocated,
            BigDecimal amountAllocated
    ) {
        BigDecimal faceValuePerUnit = faceValue.divide(
                BigDecimal.valueOf(totalUnits),
                MONEY_SCALE,
                RoundingMode.HALF_UP
        );
        BigDecimal faceValueAllocated = faceValuePerUnit.multiply(BigDecimal.valueOf(unitsAllocated))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal discountAmount = faceValueAllocated.subtract(amountAllocated)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return new YieldSnapshot(discountAmount, faceValueAllocated);
    }
}
