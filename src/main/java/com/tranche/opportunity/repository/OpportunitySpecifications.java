package com.tranche.opportunity.repository;

import com.tranche.opportunity.domain.Opportunity;
import com.tranche.opportunity.domain.OpportunityStatus;
import com.tranche.opportunity.domain.RiskGrade;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class OpportunitySpecifications {

    private OpportunitySpecifications() {
    }

    public static Specification<Opportunity> withFilters(
            OpportunityStatus status,
            RiskGrade riskGrade,
            Long issuerId
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (riskGrade != null) {
                predicates.add(criteriaBuilder.equal(root.get("riskGrade"), riskGrade));
            }
            if (issuerId != null) {
                predicates.add(criteriaBuilder.equal(root.get("issuer").get("id"), issuerId));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
