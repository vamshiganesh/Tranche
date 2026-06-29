package com.tranche.opportunity.service;

import com.tranche.audit.domain.AuditActions;
import com.tranche.audit.domain.AuditActorRole;
import com.tranche.audit.service.AuditService;
import com.tranche.auth.domain.User;
import com.tranche.auth.repository.UserRepository;
import com.tranche.common.domain.Role;
import com.tranche.common.dto.PageResponse;
import com.tranche.common.exception.BusinessException;
import com.tranche.common.exception.ErrorCode;
import com.tranche.common.exception.ForbiddenException;
import com.tranche.common.exception.ResourceNotFoundException;
import com.tranche.common.security.SecurityUtils;
import com.tranche.common.security.UserPrincipal;
import com.tranche.notification.domain.OutboxEventType;
import com.tranche.notification.service.OutboxWriter;
import com.tranche.portfolio.service.PortfolioService;
import com.tranche.issuer.domain.Issuer;
import com.tranche.issuer.repository.IssuerRepository;
import com.tranche.opportunity.domain.Opportunity;
import com.tranche.opportunity.domain.OpportunityStateMachine;
import com.tranche.opportunity.domain.OpportunityStatus;
import com.tranche.opportunity.domain.ReviewAction;
import com.tranche.opportunity.domain.RiskGrade;
import com.tranche.opportunity.dto.AdminTransitionRequest;
import com.tranche.opportunity.dto.CreateOpportunityRequest;
import com.tranche.opportunity.dto.OpportunityResponse;
import com.tranche.opportunity.dto.OpportunityStatusResponse;
import com.tranche.opportunity.dto.OpportunitySummaryResponse;
import com.tranche.opportunity.dto.ReviewOpportunityRequest;
import com.tranche.opportunity.dto.UpdateOpportunityRequest;
import com.tranche.opportunity.repository.OpportunityRepository;
import com.tranche.opportunity.repository.OpportunitySpecifications;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

@Service
public class OpportunityService {

    private static final Set<OpportunityStatus> ADMIN_TRANSITION_TARGETS = Set.of(
            OpportunityStatus.MATURED,
            OpportunityStatus.SETTLED
    );

    private final OpportunityRepository opportunityRepository;
    private final IssuerRepository issuerRepository;
    private final OpportunityStateMachine stateMachine;

    public OpportunityService(
            OpportunityRepository opportunityRepository,
            IssuerRepository issuerRepository,
            OpportunityStateMachine stateMachine
    ) {
        this.opportunityRepository = opportunityRepository;
        this.issuerRepository = issuerRepository;
        this.stateMachine = stateMachine;
    }

    @Transactional
    @CacheEvict(cacheNames = OpportunityCacheNames.LIVE_LISTINGS, allEntries = true)
    public OpportunityResponse create(CreateOpportunityRequest request, UserPrincipal principal) {
        Issuer issuer = resolveIssuerForCreate(request, principal);
        validateBusinessRules(
                request.faceValue(),
                request.minimumLot(),
                request.totalUnits(),
                request.unitPrice()
        );

        Opportunity opportunity = new Opportunity();
        opportunity.setIssuer(issuer);
        applyCreateFields(opportunity, request);
        opportunity.setStatus(OpportunityStatus.DRAFT);
        opportunity.setRemainingUnits(request.totalUnits());

        return OpportunityMapper.toResponse(opportunityRepository.save(opportunity));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = OpportunityCacheNames.LIVE_LISTINGS, allEntries = true),
            @CacheEvict(cacheNames = OpportunityCacheNames.DETAIL, key = "#id")
    })
    public OpportunityResponse update(Long id, UpdateOpportunityRequest request, UserPrincipal principal) {
        Opportunity opportunity = findOwnedOrAdmin(id, principal);
        assertEditable(opportunity);

        if (request.title() != null) {
            opportunity.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            opportunity.setDescription(request.description());
        }
        if (request.faceValue() != null) {
            opportunity.setFaceValue(request.faceValue());
        }
        if (request.discountRate() != null) {
            opportunity.setDiscountRate(request.discountRate());
        }
        if (request.tenureDays() != null) {
            opportunity.setTenureDays(request.tenureDays());
        }
        if (request.minimumLot() != null) {
            opportunity.setMinimumLot(request.minimumLot());
        }
        if (request.riskGrade() != null) {
            opportunity.setRiskGrade(request.riskGrade());
        }
        if (request.unitPrice() != null) {
            opportunity.setUnitPrice(request.unitPrice());
        }
        if (request.totalUnits() != null) {
            opportunity.setTotalUnits(request.totalUnits());
            opportunity.setRemainingUnits(request.totalUnits());
        }

        validateBusinessRules(
                opportunity.getFaceValue(),
                opportunity.getMinimumLot(),
                opportunity.getTotalUnits(),
                opportunity.getUnitPrice()
        );

        return OpportunityMapper.toResponse(opportunityRepository.save(opportunity));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = OpportunityCacheNames.LIVE_LISTINGS, allEntries = true),
            @CacheEvict(cacheNames = OpportunityCacheNames.DETAIL, key = "#id")
    })
    public OpportunityStatusResponse submitForReview(Long id, UserPrincipal principal) {
        Opportunity opportunity = findOwnedByIssuer(id, principal);
        transition(opportunity, OpportunityStatus.UNDER_REVIEW);
        return OpportunityMapper.toStatusResponse(opportunityRepository.save(opportunity));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = OpportunityCacheNames.LIVE_LISTINGS, allEntries = true),
            @CacheEvict(cacheNames = OpportunityCacheNames.DETAIL, key = "#id")
    })
    public OpportunityStatusResponse review(Long id, ReviewOpportunityRequest request) {
        Opportunity opportunity = findById(id);
        OpportunityStatus target = request.action() == ReviewAction.APPROVE
                ? OpportunityStatus.APPROVED
                : OpportunityStatus.DRAFT;

        transition(opportunity, target);
        opportunity.setReviewedAt(Instant.now());
        opportunity.setReviewComment(request.comment());

        return OpportunityMapper.toStatusResponse(opportunityRepository.save(opportunity));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = OpportunityCacheNames.LIVE_LISTINGS, allEntries = true),
            @CacheEvict(cacheNames = OpportunityCacheNames.DETAIL, key = "#id")
    })
    public OpportunityStatusResponse publish(Long id) {
        Opportunity opportunity = findById(id);
        transition(opportunity, OpportunityStatus.LIVE);

        Instant now = Instant.now();
        opportunity.setPublishedAt(now);
        opportunity.setMaturityDate(LocalDate.ofInstant(now, ZoneOffset.UTC).plusDays(opportunity.getTenureDays()));

        return OpportunityMapper.toStatusResponse(opportunityRepository.save(opportunity));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = OpportunityCacheNames.LIVE_LISTINGS, allEntries = true),
            @CacheEvict(cacheNames = OpportunityCacheNames.DETAIL, key = "#id")
    })
    public OpportunityStatusResponse adminTransition(Long id, AdminTransitionRequest request) {
        if (!ADMIN_TRANSITION_TARGETS.contains(request.targetStatus())) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Admin transition only supports target statuses: MATURED, SETTLED"
            );
        }

        Opportunity opportunity = findById(id);
        transition(opportunity, request.targetStatus());

        Instant now = Instant.now();
        if (request.targetStatus() == OpportunityStatus.MATURED) {
            opportunity.setMaturedAt(now);
        }
        if (request.targetStatus() == OpportunityStatus.SETTLED) {
            opportunity.setSettledAt(now);
        }

        return OpportunityMapper.toStatusResponse(opportunityRepository.save(opportunity));
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = OpportunityCacheNames.LIVE_LISTINGS,
            condition = "#status == T(com.tranche.opportunity.domain.OpportunityStatus).LIVE",
            key = "#riskGrade + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort"
    )
    public PageResponse<OpportunitySummaryResponse> list(
            OpportunityStatus status,
            RiskGrade riskGrade,
            Pageable pageable
    ) {
        Page<Opportunity> page = opportunityRepository.findAll(
                OpportunitySpecifications.withFilters(status, riskGrade),
                pageable
        );
        return PageResponse.from(page.map(OpportunityMapper::toSummary));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = OpportunityCacheNames.DETAIL, key = "#id")
    public OpportunityResponse getById(Long id) {
        return OpportunityMapper.toResponse(findById(id));
    }

    private void transition(Opportunity opportunity, OpportunityStatus target) {
        stateMachine.assertTransition(opportunity.getStatus(), target);
        opportunity.setStatus(target);
    }

    private Opportunity findById(Long id) {
        return opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
    }

    private Opportunity findOwnedByIssuer(Long id, UserPrincipal principal) {
        if (principal.getRole() != Role.ISSUER) {
            throw new ForbiddenException("Only the issuer owner can perform this action");
        }
        Issuer issuer = issuerRepository.findByUser_PublicId(principal.getPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Issuer profile not found"));

        return opportunityRepository.findByIdAndIssuer_Id(id, issuer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
    }

    private Opportunity findOwnedOrAdmin(Long id, UserPrincipal principal) {
        if (principal.getRole() == Role.ADMIN) {
            return findById(id);
        }
        return findOwnedByIssuer(id, principal);
    }

    private Issuer resolveIssuerForCreate(CreateOpportunityRequest request, UserPrincipal principal) {
        if (principal.getRole() == Role.ADMIN) {
            if (request.issuerId() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "issuerId is required when admin creates an opportunity");
            }
            return issuerRepository.findById(request.issuerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Issuer not found"));
        }
        if (principal.getRole() == Role.ISSUER) {
            return issuerRepository.findByUser_PublicId(principal.getPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Issuer profile not found"));
        }
        throw new ForbiddenException("Only issuers or admins can create opportunities");
    }

    private void assertEditable(Opportunity opportunity) {
        if (opportunity.getStatus() != OpportunityStatus.DRAFT) {
            throw new BusinessException(
                    ErrorCode.OPPORTUNITY_NOT_EDITABLE,
                    "Opportunity can only be edited while in DRAFT status"
            );
        }
    }

    private void applyCreateFields(Opportunity opportunity, CreateOpportunityRequest request) {
        opportunity.setTitle(request.title().trim());
        opportunity.setDescription(request.description());
        opportunity.setFaceValue(request.faceValue());
        opportunity.setDiscountRate(request.discountRate());
        opportunity.setTenureDays(request.tenureDays());
        opportunity.setMinimumLot(request.minimumLot());
        opportunity.setRiskGrade(request.riskGrade());
        opportunity.setTotalUnits(request.totalUnits());
        opportunity.setUnitPrice(request.unitPrice());
    }

    private void validateBusinessRules(
            BigDecimal faceValue,
            BigDecimal minimumLot,
            int totalUnits,
            BigDecimal unitPrice
    ) {
        BigDecimal totalOffering = unitPrice.multiply(BigDecimal.valueOf(totalUnits));
        if (minimumLot.compareTo(unitPrice) < 0) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "minimumLot must be at least one unit price (" + unitPrice + ")"
            );
        }
        if (minimumLot.compareTo(totalOffering) > 0) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "minimumLot cannot exceed total offering value"
            );
        }
        if (faceValue.compareTo(totalOffering) < 0) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "faceValue should be at least total units × unit price"
            );
        }
    }
}
