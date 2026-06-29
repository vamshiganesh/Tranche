package com.tranche.opportunity.repository;

import com.tranche.opportunity.domain.Opportunity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityGraph.EntityGraphType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {

    Optional<Opportunity> findByIdAndIssuer_Id(Long id, Long issuerId);

    /**
     * Serializes concurrent commitments on the same opportunity (SELECT ... FOR UPDATE).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Opportunity o WHERE o.id = :id")
    Optional<Opportunity> findByIdForUpdate(@Param("id") Long id);
}
