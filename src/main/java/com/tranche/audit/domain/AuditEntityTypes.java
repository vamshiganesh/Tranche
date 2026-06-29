package com.tranche.audit.domain;

/**
 * Canonical audit entity type strings — keep in sync with query APIs and timeline lookups.
 */
public final class AuditEntityTypes {

    public static final String OPPORTUNITY = "Opportunity";
    public static final String INVESTMENT_ORDER = "InvestmentOrder";
    public static final String ALLOCATION = "Allocation";
    public static final String INVESTOR_PROFILE = "InvestorProfile";
    public static final String PORTFOLIO_POSITION = "PortfolioPosition";

    private AuditEntityTypes() {
    }
}
