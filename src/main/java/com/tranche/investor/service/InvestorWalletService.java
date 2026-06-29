package com.tranche.investor.service;

import com.tranche.common.exception.BusinessException;
import com.tranche.common.exception.ErrorCode;
import com.tranche.investor.domain.InvestorProfile;

import java.math.BigDecimal;
import java.util.Map;

public final class InvestorWalletService {

    private InvestorWalletService() {
    }

    /**
     * Moves funds from available wallet balance to locked balance within the allocation transaction.
     * Caller must already hold a pessimistic lock on the investor profile row.
     */
    public static void lockFunds(InvestorProfile profile, BigDecimal amount) {
        if (profile.getWalletBalance().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS, "Insufficient wallet balance");
        }
        profile.setWalletBalance(profile.getWalletBalance().subtract(amount));
        profile.setLockedBalance(profile.getLockedBalance().add(amount));
    }

    public static Map<String, Object> walletSnapshot(InvestorProfile profile) {
        return Map.of(
                "walletBalance", profile.getWalletBalance(),
                "lockedBalance", profile.getLockedBalance()
        );
    }
}
