package com.tranche.investor.dto;

import java.math.BigDecimal;

public record DemoCreditResponse(
        BigDecimal creditedAmount,
        BigDecimal walletBalance
) {
}
