package com.tranche.portfolio.repository;

import com.tranche.portfolio.domain.PortfolioPosition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioPositionRepository extends JpaRepository<PortfolioPosition, Long> {
}
