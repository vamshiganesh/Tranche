-- Demo opportunity for local walkthroughs and integration tests.
-- Lifecycle: DRAFT → submit → review → publish → invest → mature → settle

SET NAMES utf8mb4;

SET @issuer_id = (SELECT id FROM issuers WHERE company_name = 'Acme Corp' LIMIT 1);

INSERT INTO opportunities (
    issuer_id,
    title,
    description,
    face_value,
    discount_rate,
    tenure_days,
    minimum_lot,
    risk_grade,
    total_units,
    remaining_units,
    unit_price,
    status,
    maturity_date
)
VALUES (
    @issuer_id,
    'Acme Q1 Receivables — Invoice #INV-2026-0142',
    'Discounted trade receivable from Acme Corp. Net-90 terms, investment-grade counterparty exposure.',
    2500000.0000,
    7.2500,
    90,
    25000.0000,
    'A',
    100,
    100,
    25000.0000,
    'DRAFT',
    DATE_ADD(CURDATE(), INTERVAL 90 DAY)
);
