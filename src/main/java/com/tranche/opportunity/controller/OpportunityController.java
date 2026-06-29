package com.tranche.opportunity.controller;

import com.tranche.common.dto.PageResponse;
import com.tranche.common.security.SecurityUtils;
import com.tranche.opportunity.domain.OpportunityStatus;
import com.tranche.opportunity.domain.RiskGrade;
import com.tranche.opportunity.dto.AdminTransitionRequest;
import com.tranche.opportunity.dto.CreateOpportunityRequest;
import com.tranche.opportunity.dto.OpportunityResponse;
import com.tranche.opportunity.dto.OpportunityStatusResponse;
import com.tranche.opportunity.dto.OpportunitySummaryResponse;
import com.tranche.opportunity.dto.ReviewOpportunityRequest;
import com.tranche.opportunity.dto.UpdateOpportunityRequest;
import com.tranche.opportunity.service.OpportunityService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/opportunities")
public class OpportunityController {

    private final OpportunityService opportunityService;

    public OpportunityController(OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ISSUER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public OpportunityResponse create(@Valid @RequestBody CreateOpportunityRequest request) {
        return opportunityService.create(request, SecurityUtils.requireCurrentUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ISSUER', 'ADMIN')")
    public OpportunityResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOpportunityRequest request
    ) {
        return opportunityService.update(id, request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('ISSUER')")
    public OpportunityStatusResponse submit(@PathVariable Long id) {
        return opportunityService.submitForReview(id, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public OpportunityStatusResponse review(
            @PathVariable Long id,
            @Valid @RequestBody ReviewOpportunityRequest request
    ) {
        return opportunityService.review(id, request);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public OpportunityStatusResponse publish(@PathVariable Long id) {
        return opportunityService.publish(id);
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasRole('ADMIN')")
    public OpportunityStatusResponse transition(
            @PathVariable Long id,
            @Valid @RequestBody AdminTransitionRequest request
    ) {
        return opportunityService.adminTransition(id, request);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<OpportunitySummaryResponse> list(
            @RequestParam(required = false) OpportunityStatus status,
            @RequestParam(required = false) RiskGrade riskGrade,
            @PageableDefault(size = 20, sort = "maturityDate", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return opportunityService.list(status, riskGrade, pageable, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public OpportunityResponse getById(@PathVariable Long id) {
        return opportunityService.getById(id);
    }
}
