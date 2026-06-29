package com.tranche.opportunity.repository;

import com.tranche.opportunity.domain.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {

    Optional<Opportunity> findByIdAndIssuer_Id(Long id, Long issuerId);
}
