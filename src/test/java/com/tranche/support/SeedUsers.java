package com.tranche.support;

/**
 * Seeded users from Flyway V2. Password for all: {@link #PASSWORD}.
 */
public final class SeedUsers {

    public static final String PASSWORD = "Password123!";

    public static final String ADMIN_EMAIL = "admin@tranche.local";
    public static final String ISSUER_EMAIL = "issuer@tranche.local";
    public static final String INVESTOR1_EMAIL = "investor1@tranche.local";
    public static final String INVESTOR2_EMAIL = "investor2@tranche.local";

    public static final String DEMO_OPPORTUNITY_TITLE =
            "Acme Q1 Receivables — Invoice #INV-2026-0142";

    private SeedUsers() {
    }
}
