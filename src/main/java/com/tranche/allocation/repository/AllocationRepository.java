package com.tranche.allocation.repository;

import com.tranche.allocation.domain.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AllocationRepository extends JpaRepository<Allocation, Long> {

    @Query("SELECT COALESCE(SUM(a.units), 0) FROM Allocation a WHERE a.opportunity.id = :opportunityId")
    int sumUnitsByOpportunityId(@Param("opportunityId") Long opportunityId);
}
