package com.tranche.allocation.service;

import com.tranche.allocation.domain.InvestmentOrder;
import com.tranche.allocation.dto.CommitmentResponse;

final class CommitmentMapper {

    private CommitmentMapper() {
    }

    static CommitmentResponse toResponse(InvestmentOrder order) {
        return new CommitmentResponse(
                order.getId(),
                order.getOpportunity().getId(),
                order.getUnitsRequested(),
                order.getUnitsAllocated(),
                order.getAmountRequested(),
                order.getAmountAllocated(),
                order.getFillStatus(),
                order.getStatus(),
                order.getIdempotencyKey(),
                order.getCreatedAt()
        );
    }
}
