package com.tranche.investor.controller;

import com.tranche.common.security.SecurityUtils;
import com.tranche.investor.dto.DemoCreditResponse;
import com.tranche.investor.service.InvestorWalletCreditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/investors")
public class InvestorController {

    private final InvestorWalletCreditService investorWalletCreditService;

    public InvestorController(InvestorWalletCreditService investorWalletCreditService) {
        this.investorWalletCreditService = investorWalletCreditService;
    }

    @PostMapping("/wallet/demo-credit")
    @PreAuthorize("hasRole('INVESTOR')")
    public DemoCreditResponse applyDemoCredit() {
        return investorWalletCreditService.applyDemoCredit(SecurityUtils.requireCurrentUser());
    }
}
