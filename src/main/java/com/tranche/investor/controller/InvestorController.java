package com.tranche.investor.controller;

import com.tranche.common.security.SecurityUtils;
import com.tranche.investor.dto.DemoCreditResponse;
import com.tranche.investor.service.InvestorKycService;
import com.tranche.investor.service.InvestorWalletCreditService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/investors")
public class InvestorController {

    private final InvestorWalletCreditService investorWalletCreditService;
    private final InvestorKycService investorKycService;

    public InvestorController(
            InvestorWalletCreditService investorWalletCreditService,
            InvestorKycService investorKycService
    ) {
        this.investorWalletCreditService = investorWalletCreditService;
        this.investorKycService = investorKycService;
    }

    @PostMapping("/wallet/demo-credit")
    @PreAuthorize("hasRole('INVESTOR')")
    public DemoCreditResponse applyDemoCredit() {
        return investorWalletCreditService.applyDemoCredit(SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/kyc/resubmit")
    @PreAuthorize("hasRole('INVESTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resubmitKyc() {
        investorKycService.resubmitKyc(SecurityUtils.requireCurrentUser());
    }
}
