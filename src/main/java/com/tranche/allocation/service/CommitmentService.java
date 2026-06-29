package com.tranche.allocation.service;

import com.tranche.allocation.dto.CommitmentRequest;
import com.tranche.allocation.dto.CommitmentResult;
import com.tranche.common.exception.BusinessException;
import com.tranche.common.exception.ErrorCode;
import com.tranche.common.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CommitmentService {

    private final AllocationEngine allocationEngine;

    public CommitmentService(AllocationEngine allocationEngine) {
        this.allocationEngine = allocationEngine;
    }

    public CommitmentResult placeCommitment(
            Long opportunityId,
            String idempotencyKeyHeader,
            CommitmentRequest request,
            UserPrincipal investor
    ) {
        UUID idempotencyKey = parseIdempotencyKey(idempotencyKeyHeader);
        return allocationEngine.allocate(opportunityId, idempotencyKey, request, investor);
    }

    private static UUID parseIdempotencyKey(String header) {
        if (header == null || header.isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_IDEMPOTENCY_KEY, "Idempotency-Key header is required");
        }
        try {
            return UUID.fromString(header.trim());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Idempotency-Key must be a valid UUID");
        }
    }
}
