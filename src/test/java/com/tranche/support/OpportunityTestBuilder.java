package com.tranche.support;

import com.tranche.issuer.domain.Issuer;
import com.tranche.opportunity.domain.Opportunity;
import com.tranche.opportunity.domain.OpportunityStatus;
import com.tranche.opportunity.domain.RiskGrade;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Builds realistic opportunity entities for integration tests.
 */
public final class OpportunityTestBuilder {

    private Issuer issuer;
    private String title = "Test invoice receivable";
    private BigDecimal faceValue = new BigDecimal("1000000.0000");
    private BigDecimal discountRate = new BigDecimal("8.5000");
    private int tenureDays = 90;
    private BigDecimal minimumLot = new BigDecimal("10000.0000");
    private RiskGrade riskGrade = RiskGrade.A;
    private int totalUnits = 100;
    private int remainingUnits = 100;
    private BigDecimal unitPrice = new BigDecimal("10000.0000");
    private OpportunityStatus status = OpportunityStatus.LIVE;
    private LocalDate maturityDate = LocalDate.now().plusDays(90);

    public static OpportunityTestBuilder anOpportunity() {
        return new OpportunityTestBuilder();
    }

    public OpportunityTestBuilder issuer(Issuer issuer) {
        this.issuer = issuer;
        return this;
    }

    public OpportunityTestBuilder title(String title) {
        this.title = title;
        return this;
    }

    public OpportunityTestBuilder totalUnits(int totalUnits) {
        this.totalUnits = totalUnits;
        this.remainingUnits = totalUnits;
        return this;
    }

    public OpportunityTestBuilder remainingUnits(int remainingUnits) {
        this.remainingUnits = remainingUnits;
        return this;
    }

    public OpportunityTestBuilder unitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        return this;
    }

    public OpportunityTestBuilder status(OpportunityStatus status) {
        this.status = status;
        return this;
    }

    public OpportunityTestBuilder draft() {
        this.status = OpportunityStatus.DRAFT;
        return this;
    }

    public OpportunityTestBuilder live() {
        this.status = OpportunityStatus.LIVE;
        return this;
    }

    public Opportunity build() {
        if (issuer == null) {
            throw new IllegalStateException("issuer is required");
        }
        Opportunity opportunity = new Opportunity();
        opportunity.setIssuer(issuer);
        opportunity.setTitle(title);
        opportunity.setFaceValue(faceValue);
        opportunity.setDiscountRate(discountRate);
        opportunity.setTenureDays(tenureDays);
        opportunity.setMinimumLot(minimumLot);
        opportunity.setRiskGrade(riskGrade);
        opportunity.setTotalUnits(totalUnits);
        opportunity.setRemainingUnits(remainingUnits);
        opportunity.setUnitPrice(unitPrice);
        opportunity.setStatus(status);
        opportunity.setMaturityDate(maturityDate);
        return opportunity;
    }
}
