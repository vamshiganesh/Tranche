package com.tranche.allocation.service;

import com.tranche.allocation.domain.Allocation;
import com.tranche.allocation.domain.AllocationCalculator;
import com.tranche.allocation.domain.FillStatus;
import com.tranche.allocation.domain.InvestmentOrder;
import com.tranche.allocation.domain.OrderStatus;
import com.tranche.allocation.dto.CommitmentRequest;
import com.tranche.allocation.dto.CommitmentResponse;
import com.tranche.allocation.dto.CommitmentResult;
import com.tranche.allocation.repository.AllocationRepository;
import com.tranche.allocation.repository.InvestmentOrderRepository;
import com.tranche.audit.domain.AuditActions;
import com.tranche.audit.domain.AuditActorRole;
import com.tranche.audit.service.AuditService;
import com.tranche.auth.domain.User;
import com.tranche.auth.repository.UserRepository;
import com.tranche.common.exception.BusinessException;
import com.tranche.common.exception.ErrorCode;
import com.tranche.common.exception.ResourceNotFoundException;
import com.tranche.common.security.UserPrincipal;
import com.tranche.common.util.CorrelationIdHolder;
import com.tranche.investor.domain.InvestorProfile;
import com.tranche.investor.repository.InvestorProfileRepository;
import com.tranche.investor.service.InvestorWalletService;
import com.tranche.notification.domain.OutboxEventType;
import com.tranche.notification.service.OutboxWriter;
import com.tranche.opportunity.domain.Opportunity;
import com.tranche.opportunity.domain.OpportunityStateMachine;
import com.tranche.opportunity.domain.OpportunityStatus;
import com.tranche.opportunity.repository.OpportunityRepository;
import com.tranche.opportunity.service.OpportunityCacheNames;
import com.tranche.portfolio.domain.PortfolioPosition;
import com.tranche.portfolio.domain.PortfolioStatus;
import com.tranche.portfolio.repository.PortfolioPositionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AllocationEngine {

    private final OpportunityRepository opportunityRepository;
    private final InvestorProfileRepository investorProfileRepository;
    private final InvestmentOrderRepository investmentOrderRepository;
    private final AllocationRepository allocationRepository;
    private final PortfolioPositionRepository portfolioPositionRepository;
    private final UserRepository userRepository;
    private final OpportunityStateMachine stateMachine;
    private final AuditService auditService;
    private final OutboxWriter outboxWriter;

    public AllocationEngine(
            OpportunityRepository opportunityRepository,
            InvestorProfileRepository investorProfileRepository,
            InvestmentOrderRepository investmentOrderRepository,
            AllocationRepository allocationRepository,
            PortfolioPositionRepository portfolioPositionRepository,
            UserRepository userRepository,
            OpportunityStateMachine stateMachine,
            AuditService auditService,
            OutboxWriter outboxWriter
    ) {
        this.opportunityRepository = opportunityRepository;
        this.investorProfileRepository = investorProfileRepository;
        this.investmentOrderRepository = investmentOrderRepository;
        this.allocationRepository = allocationRepository;
        this.portfolioPositionRepository = portfolioPositionRepository;
        this.userRepository = userRepository;
        this.stateMachine = stateMachine;
        this.auditService = auditService;
        this.outboxWriter = outboxWriter;
    }

    /**
     * Single transactional boundary for commitment processing.
     * Lock order: opportunity row first, then investor profile — consistent across all threads.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = OpportunityCacheNames.LIVE_LISTINGS, allEntries = true),
            @CacheEvict(cacheNames = OpportunityCacheNames.DETAIL, key = "#opportunityId")
    })
    public CommitmentResult allocate(
            Long opportunityId,
            UUID idempotencyKey,
            CommitmentRequest request,
            UserPrincipal investor
    ) {
        String idempotencyKeyValue = idempotencyKey.toString();

        var existing = investmentOrderRepository.findByIdempotencyKeyAndInvestor_Id(
                idempotencyKeyValue,
                investor.getId()
        );
        if (existing.isPresent()) {
            return handleExistingOrder(existing.get());
        }

        try {
            return processNewCommitment(opportunityId, idempotencyKeyValue, request, investor);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent duplicate idempotency key: the winner committed first.
            InvestmentOrder winner = investmentOrderRepository
                    .findByIdempotencyKeyAndInvestor_Id(idempotencyKeyValue, investor.getId())
                    .orElseThrow(() -> ex);
            return handleExistingOrder(winner);
        }
    }

    private CommitmentResult handleExistingOrder(InvestmentOrder order) {
        if (order.getStatus() == OrderStatus.REJECTED) {
            throw new BusinessException(
                    ErrorCode.valueOf(order.getRejectionReason()),
                    rejectionMessage(ErrorCode.valueOf(order.getRejectionReason()))
            );
        }
        return new CommitmentResult(toResponse(order), true);
    }

    private CommitmentResult processNewCommitment(
            Long opportunityId,
            String idempotencyKey,
            CommitmentRequest request,
            UserPrincipal investorPrincipal
    ) {
        Opportunity opportunity = opportunityRepository.findByIdForUpdate(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));

        if (opportunity.getStatus() != OpportunityStatus.LIVE) {
            throw new BusinessException(
                    ErrorCode.OPPORTUNITY_NOT_LIVE,
                    "Opportunity is not accepting commitments"
            );
        }

        AllocationCalculator.validateRequestedAmount(
                request.unitsRequested(),
                opportunity.getUnitPrice(),
                request.amount()
        );
        AllocationCalculator.validateMinimumLot(request.amount(), opportunity.getMinimumLot());

        AllocationCalculator.FillDecision fill = AllocationCalculator.computeFill(
                request.unitsRequested(),
                opportunity.getRemainingUnits(),
                opportunity.getUnitPrice(),
                opportunity.getMinimumLot()
        );

        User investor = userRepository.getReferenceById(investorPrincipal.getId());

        if (fill.rejected()) {
            InvestmentOrder rejectedOrder = persistRejectedOrder(
                    opportunity,
                    investor,
                    idempotencyKey,
                    request,
                    fill
            );
            auditService.log(
                    investor,
                    AuditActorRole.INVESTOR,
                    AuditActions.COMMITMENT_REJECTED,
                    "InvestmentOrder",
                    rejectedOrder.getId(),
                    Map.of("opportunityId", opportunityId),
                    Map.of(
                            "fillStatus", FillStatus.REJECTED.name(),
                            "reason", fill.rejectionCode().name()
                    )
            );
            throw new BusinessException(fill.rejectionCode(), fill.rejectionMessage());
        }

        // Second lock: investor wallet. Always after opportunity lock.
        InvestorProfile profile = investorProfileRepository.findByUserIdForUpdate(investorPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Investor profile not found"));

        Map<String, Object> walletBefore = InvestorWalletService.walletSnapshot(profile);
        try {
            InvestorWalletService.lockFunds(profile, fill.amountAllocated());
        } catch (BusinessException ex) {
            if (ex.getCode() == ErrorCode.INSUFFICIENT_FUNDS) {
                InvestmentOrder rejectedOrder = persistRejectedOrder(
                        opportunity,
                        investor,
                        idempotencyKey,
                        request,
                        AllocationCalculator.FillDecision.rejected(
                                ErrorCode.INSUFFICIENT_FUNDS,
                                ex.getMessage()
                        )
                );
                auditService.log(
                        investor,
                        AuditActorRole.INVESTOR,
                        AuditActions.COMMITMENT_REJECTED,
                        "InvestmentOrder",
                        rejectedOrder.getId(),
                        walletBefore,
                        Map.of("reason", ErrorCode.INSUFFICIENT_FUNDS.name())
                );
            }
            throw ex;
        }
        investorProfileRepository.save(profile);

        int remainingBefore = opportunity.getRemainingUnits();
        opportunity.setRemainingUnits(remainingBefore - fill.unitsAllocated());

        OpportunityStatus statusBefore = opportunity.getStatus();
        if (opportunity.getRemainingUnits() == 0) {
            stateMachine.assertTransition(statusBefore, OpportunityStatus.FULLY_SUBSCRIBED);
            opportunity.setStatus(OpportunityStatus.FULLY_SUBSCRIBED);
        }

        InvestmentOrder order = new InvestmentOrder();
        order.setOpportunity(opportunity);
        order.setInvestor(investor);
        order.setIdempotencyKey(idempotencyKey);
        order.setUnitsRequested(request.unitsRequested());
        order.setUnitsAllocated(fill.unitsAllocated());
        order.setAmountRequested(request.amount());
        order.setAmountAllocated(fill.amountAllocated());
        order.setFillStatus(fill.fillStatus());
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCorrelationId(CorrelationIdHolder.get());
        order = investmentOrderRepository.save(order);

        AllocationCalculator.YieldSnapshot yield = AllocationCalculator.computeYield(
                opportunity.getFaceValue(),
                opportunity.getTotalUnits(),
                fill.unitsAllocated(),
                fill.amountAllocated()
        );

        Allocation allocation = new Allocation();
        allocation.setInvestmentOrder(order);
        allocation.setOpportunity(opportunity);
        allocation.setInvestor(investor);
        allocation.setUnits(fill.unitsAllocated());
        allocation.setAmount(fill.amountAllocated());
        allocation.setDiscountAmount(yield.discountAmount());
        allocation.setExpectedReturn(yield.expectedReturn());
        allocation.setAllocatedAt(Instant.now());
        allocation = allocationRepository.save(allocation);

        PortfolioPosition position = new PortfolioPosition();
        position.setInvestor(investor);
        position.setOpportunity(opportunity);
        position.setAllocation(allocation);
        position.setInvestedAmount(fill.amountAllocated());
        position.setExpectedReturn(yield.expectedReturn());
        position.setMaturityDate(opportunity.getMaturityDate());
        position.setStatus(PortfolioStatus.ACTIVE);
        portfolioPositionRepository.save(position);

        opportunityRepository.save(opportunity);

        auditService.log(
                investor,
                AuditActorRole.INVESTOR,
                AuditActions.FUNDS_LOCKED,
                "InvestorProfile",
                profile.getId(),
                walletBefore,
                InvestorWalletService.walletSnapshot(profile)
        );

        auditService.log(
                investor,
                AuditActorRole.INVESTOR,
                AuditActions.ALLOCATION_CREATED,
                "Allocation",
                allocation.getId(),
                Map.of(
                        "opportunityId", opportunityId,
                        "remainingUnitsBefore", remainingBefore
                ),
                Map.of(
                        "units", fill.unitsAllocated(),
                        "amount", fill.amountAllocated(),
                        "remainingUnitsAfter", opportunity.getRemainingUnits(),
                        "orderId", order.getId()
                )
        );

        if (opportunity.getStatus() == OpportunityStatus.FULLY_SUBSCRIBED) {
            auditService.log(
                    investor,
                    AuditActorRole.SYSTEM,
                    AuditActions.OPPORTUNITY_FULLY_SUBSCRIBED,
                    "Opportunity",
                    opportunityId,
                    Map.of("status", statusBefore.name()),
                    Map.of("status", OpportunityStatus.FULLY_SUBSCRIBED.name())
            );
        }

        outboxWriter.write(
                OutboxEventType.INVESTMENT_SUCCESSFUL,
                "Allocation",
                allocation.getId(),
                Map.of(
                        "allocationId", allocation.getId(),
                        "orderId", order.getId(),
                        "opportunityId", opportunityId,
                        "investorId", investor.getId(),
                        "units", fill.unitsAllocated(),
                        "amount", fill.amountAllocated(),
                        "fillStatus", fill.fillStatus().name()
                )
        );

        return new CommitmentResult(toResponse(order), false);
    }

    private InvestmentOrder persistRejectedOrder(
            Opportunity opportunity,
            User investor,
            String idempotencyKey,
            CommitmentRequest request,
            AllocationCalculator.FillDecision fill
    ) {
        InvestmentOrder order = new InvestmentOrder();
        order.setOpportunity(opportunity);
        order.setInvestor(investor);
        order.setIdempotencyKey(idempotencyKey);
        order.setUnitsRequested(request.unitsRequested());
        order.setUnitsAllocated(0);
        order.setAmountRequested(request.amount());
        order.setAmountAllocated(BigDecimal.ZERO);
        order.setFillStatus(FillStatus.REJECTED);
        order.setStatus(OrderStatus.REJECTED);
        order.setRejectionReason(fill.rejectionCode().name());
        order.setCorrelationId(CorrelationIdHolder.get());
        return investmentOrderRepository.save(order);
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

    private static String rejectionMessage(ErrorCode code) {
        return switch (code) {
            case INSUFFICIENT_UNITS -> "No units available for allocation";
            case BELOW_MINIMUM_LOT -> "Allocated amount is below the minimum lot size";
            case INSUFFICIENT_FUNDS -> "Insufficient wallet balance";
            case OPPORTUNITY_NOT_LIVE -> "Opportunity is not accepting commitments";
            default -> code.name();
        };
    }
}
