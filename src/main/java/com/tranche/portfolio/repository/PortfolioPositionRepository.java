package com.tranche.portfolio.repository;

import com.tranche.portfolio.domain.PortfolioPosition;
import com.tranche.portfolio.domain.PortfolioStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PortfolioPositionRepository extends JpaRepository<PortfolioPosition, Long> {

    @Query("""
            SELECT p FROM PortfolioPosition p
            JOIN FETCH p.opportunity o
            JOIN FETCH p.allocation a
            WHERE p.investor.id = :investorId
            ORDER BY p.maturityDate ASC, p.id ASC
            """)
    List<PortfolioPosition> findAllByInvestorIdWithDetails(@Param("investorId") Long investorId);

    @Query("""
            SELECT p FROM PortfolioPosition p
            JOIN FETCH p.opportunity o
            JOIN FETCH p.allocation a
            WHERE p.id = :positionId AND p.investor.id = :investorId
            """)
    Optional<PortfolioPosition> findByIdAndInvestorIdWithDetails(
            @Param("positionId") Long positionId,
            @Param("investorId") Long investorId
    );

    List<PortfolioPosition> findByOpportunity_Id(Long opportunityId);

    long countByInvestor_IdAndStatus(Long investorId, PortfolioStatus status);
}
