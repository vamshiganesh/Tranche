package com.tranche.audit.domain;

public final class AuditActions {

    public static final String OPPORTUNITY_CREATED = "OPPORTUNITY_CREATED";
    public static final String OPPORTUNITY_UPDATED = "OPPORTUNITY_UPDATED";
    public static final String OPPORTUNITY_SUBMITTED = "OPPORTUNITY_SUBMITTED";
    public static final String OPPORTUNITY_APPROVED = "OPPORTUNITY_APPROVED";
    public static final String OPPORTUNITY_REJECTED = "OPPORTUNITY_REJECTED";
    public static final String OPPORTUNITY_PUBLISHED = "OPPORTUNITY_PUBLISHED";
    public static final String OPPORTUNITY_MATURED = "OPPORTUNITY_MATURED";
    public static final String OPPORTUNITY_SETTLED = "OPPORTUNITY_SETTLED";
    public static final String OPPORTUNITY_FULLY_SUBSCRIBED = "OPPORTUNITY_FULLY_SUBSCRIBED";

    public static final String FUNDS_LOCKED = "FUNDS_LOCKED";
    public static final String ALLOCATION_CREATED = "ALLOCATION_CREATED";
    public static final String COMMITMENT_REJECTED = "COMMITMENT_REJECTED";

    public static final String PORTFOLIO_POSITION_MATURED = "PORTFOLIO_POSITION_MATURED";
    public static final String PORTFOLIO_POSITION_SETTLED = "PORTFOLIO_POSITION_SETTLED";

    private AuditActions() {
    }
}
