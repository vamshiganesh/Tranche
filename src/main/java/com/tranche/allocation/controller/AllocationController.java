package com.tranche.allocation.controller;

import com.tranche.allocation.dto.CommitmentRequest;
import com.tranche.allocation.dto.CommitmentResponse;
import com.tranche.allocation.dto.CommitmentResult;
import com.tranche.allocation.service.CommitmentService;
import com.tranche.common.security.SecurityUtils;
import com.tranche.common.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/opportunities")
public class AllocationController {

    private final CommitmentService commitmentService;

    public AllocationController(CommitmentService commitmentService) {
        this.commitmentService = commitmentService;
    }

    @PostMapping("/{id}/commitments")
    @PreAuthorize("hasRole('INVESTOR')")
    public ResponseEntity<CommitmentResponse> placeCommitment(
            @PathVariable Long id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CommitmentRequest request
    ) {
        CommitmentResult result = commitmentService.placeCommitment(
                id,
                idempotencyKey,
                request,
                currentUser()
        );
        HttpStatus status = result.replay() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result.response());
    }

    private UserPrincipal currentUser() {
        return SecurityUtils.requireCurrentUser();
    }
}
