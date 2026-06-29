package com.tranche.portfolio.controller;

import com.tranche.common.security.SecurityUtils;
import com.tranche.portfolio.dto.PortfolioPositionDetail;
import com.tranche.portfolio.dto.PortfolioResponse;
import com.tranche.portfolio.service.PortfolioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    @PreAuthorize("hasRole('INVESTOR')")
    public PortfolioResponse getPortfolio() {
        return portfolioService.getPortfolio(SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/positions/{positionId}")
    @PreAuthorize("hasRole('INVESTOR')")
    public PortfolioPositionDetail getPosition(@PathVariable Long positionId) {
        return portfolioService.getPosition(positionId, SecurityUtils.requireCurrentUser());
    }
}
