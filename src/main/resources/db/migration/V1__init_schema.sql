-- Tranche initial schema (MariaDB 11.x)
-- Invoice discounting: opportunities, commitments, allocations, audit, outbox

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    public_id       CHAR(36)            NOT NULL,
    email           VARCHAR(255)        NOT NULL,
    password_hash   VARCHAR(255)        NOT NULL,
    full_name       VARCHAR(255)        NOT NULL,
    role            ENUM('ADMIN', 'ISSUER', 'INVESTOR') NOT NULL,
    enabled         TINYINT(1)          NOT NULL DEFAULT 1,
    created_at      TIMESTAMP(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version         INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_public_id (public_id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- investor_profiles (1:1 with users where role = INVESTOR)
-- Wallet balance is locked pessimistically during allocation.
-- ---------------------------------------------------------------------------
CREATE TABLE investor_profiles (
    id              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED     NOT NULL,
    wallet_balance  DECIMAL(19, 4)      NOT NULL DEFAULT 0.0000,
    locked_balance  DECIMAL(19, 4)      NOT NULL DEFAULT 0.0000,
    currency        CHAR(3)             NOT NULL DEFAULT 'USD',
    created_at      TIMESTAMP(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version         INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_investor_profiles_user_id (user_id),
    CONSTRAINT fk_investor_profiles_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_investor_wallet_balance_non_negative
        CHECK (wallet_balance >= 0),
    CONSTRAINT chk_investor_locked_balance_non_negative
        CHECK (locked_balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- issuers (1:1 with users where role = ISSUER)
-- ---------------------------------------------------------------------------
CREATE TABLE issuers (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT UNSIGNED NOT NULL,
    company_name            VARCHAR(255)    NOT NULL,
    registration_number     VARCHAR(100)    NULL,
    created_at              TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_issuers_user_id (user_id),
    CONSTRAINT fk_issuers_user
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- opportunities
-- remaining_units is decremented atomically under pessimistic row lock.
-- version supports optimistic checks outside the allocation critical path.
-- ---------------------------------------------------------------------------
CREATE TABLE opportunities (
    id                  BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    issuer_id           BIGINT UNSIGNED     NOT NULL,
    title               VARCHAR(500)        NOT NULL,
    description         TEXT                NULL,
    face_value          DECIMAL(19, 4)      NOT NULL,
    discount_rate       DECIMAL(8, 4)       NOT NULL,
    tenure_days         INT UNSIGNED        NOT NULL,
    minimum_lot         DECIMAL(19, 4)      NOT NULL,
    risk_grade          ENUM('A', 'B', 'C', 'D') NOT NULL,
    total_units         INT UNSIGNED        NOT NULL,
    remaining_units     INT UNSIGNED        NOT NULL,
    unit_price          DECIMAL(19, 4)      NOT NULL,
    status              ENUM(
                            'DRAFT',
                            'UNDER_REVIEW',
                            'APPROVED',
                            'LIVE',
                            'FULLY_SUBSCRIBED',
                            'MATURED',
                            'SETTLED'
                        )                   NOT NULL DEFAULT 'DRAFT',
    maturity_date       DATE                NULL,
    review_comment      VARCHAR(1000)       NULL,
    published_at        TIMESTAMP(6)        NULL,
    reviewed_at         TIMESTAMP(6)        NULL,
    matured_at          TIMESTAMP(6)        NULL,
    settled_at          TIMESTAMP(6)        NULL,
    created_at          TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version             INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_opportunities_issuer
        FOREIGN KEY (issuer_id) REFERENCES issuers (id),
    CONSTRAINT chk_opportunities_face_value_positive
        CHECK (face_value > 0),
    CONSTRAINT chk_opportunities_discount_rate
        CHECK (discount_rate >= 0 AND discount_rate <= 100),
    CONSTRAINT chk_opportunities_tenure_positive
        CHECK (tenure_days > 0),
    CONSTRAINT chk_opportunities_minimum_lot_positive
        CHECK (minimum_lot > 0),
    CONSTRAINT chk_opportunities_total_units_positive
        CHECK (total_units > 0),
    CONSTRAINT chk_opportunities_remaining_units_bounds
        CHECK (remaining_units <= total_units),
    CONSTRAINT chk_opportunities_remaining_units_non_negative
        CHECK (remaining_units >= 0),
    CONSTRAINT chk_opportunities_unit_price_positive
        CHECK (unit_price > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Listing filter: status + risk_grade + maturity_date (per architecture)
CREATE INDEX idx_opportunities_listing
    ON opportunities (status, risk_grade, maturity_date);

-- Issuer dashboard: opportunities by issuer and status
CREATE INDEX idx_opportunities_issuer_status
    ON opportunities (issuer_id, status);

-- ---------------------------------------------------------------------------
-- investment_orders
-- Idempotency: unique (idempotency_key, investor_id) prevents double commitment.
-- ---------------------------------------------------------------------------
CREATE TABLE investment_orders (
    id                  BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    opportunity_id      BIGINT UNSIGNED     NOT NULL,
    investor_id         BIGINT UNSIGNED     NOT NULL,
    idempotency_key     CHAR(36)            NOT NULL,
    units_requested     INT UNSIGNED        NOT NULL,
    units_allocated     INT UNSIGNED        NOT NULL DEFAULT 0,
    amount_requested    DECIMAL(19, 4)      NOT NULL,
    amount_allocated    DECIMAL(19, 4)      NOT NULL DEFAULT 0,
    fill_status         ENUM('FULL', 'PARTIAL', 'REJECTED') NOT NULL,
    status              ENUM('PENDING', 'CONFIRMED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    rejection_reason    VARCHAR(100)        NULL,
    correlation_id      VARCHAR(64)         NULL,
    created_at          TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version             INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_investment_orders_idempotency (idempotency_key, investor_id),
    KEY idx_investment_orders_history (opportunity_id, investor_id, created_at),
    KEY idx_investment_orders_opportunity_created (opportunity_id, created_at),
    KEY idx_investment_orders_investor_created (investor_id, created_at),
    CONSTRAINT fk_investment_orders_opportunity
        FOREIGN KEY (opportunity_id) REFERENCES opportunities (id),
    CONSTRAINT fk_investment_orders_investor
        FOREIGN KEY (investor_id) REFERENCES users (id),
    CONSTRAINT chk_investment_orders_units_requested_positive
        CHECK (units_requested > 0),
    CONSTRAINT chk_investment_orders_amount_requested_positive
        CHECK (amount_requested > 0),
    CONSTRAINT chk_investment_orders_units_allocated_bounds
        CHECK (units_allocated <= units_requested),
    CONSTRAINT chk_investment_orders_amount_allocated_non_negative
        CHECK (amount_allocated >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- allocations
-- One allocation row per confirmed investment order (1:1).
-- ---------------------------------------------------------------------------
CREATE TABLE allocations (
    id                  BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    investment_order_id BIGINT UNSIGNED     NOT NULL,
    opportunity_id      BIGINT UNSIGNED     NOT NULL,
    investor_id         BIGINT UNSIGNED     NOT NULL,
    units               INT UNSIGNED        NOT NULL,
    amount              DECIMAL(19, 4)      NOT NULL,
    discount_amount     DECIMAL(19, 4)      NOT NULL,
    expected_return     DECIMAL(19, 4)      NOT NULL,
    allocated_at        TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at          TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_allocations_investment_order (investment_order_id),
    KEY idx_allocations_investor_created (investor_id, created_at),
    KEY idx_allocations_opportunity (opportunity_id),
    CONSTRAINT fk_allocations_investment_order
        FOREIGN KEY (investment_order_id) REFERENCES investment_orders (id),
    CONSTRAINT fk_allocations_opportunity
        FOREIGN KEY (opportunity_id) REFERENCES opportunities (id),
    CONSTRAINT fk_allocations_investor
        FOREIGN KEY (investor_id) REFERENCES users (id),
    CONSTRAINT chk_allocations_units_positive
        CHECK (units > 0),
    CONSTRAINT chk_allocations_amount_positive
        CHECK (amount > 0),
    CONSTRAINT chk_allocations_discount_amount_non_negative
        CHECK (discount_amount >= 0),
    CONSTRAINT chk_allocations_expected_return_positive
        CHECK (expected_return > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- portfolio_positions
-- Materialized view of investor holdings; updated on successful allocation.
-- ---------------------------------------------------------------------------
CREATE TABLE portfolio_positions (
    id                  BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    investor_id         BIGINT UNSIGNED     NOT NULL,
    opportunity_id      BIGINT UNSIGNED     NOT NULL,
    allocation_id       BIGINT UNSIGNED     NOT NULL,
    invested_amount     DECIMAL(19, 4)      NOT NULL,
    expected_return     DECIMAL(19, 4)      NOT NULL,
    realized_yield      DECIMAL(8, 4)       NULL,
    maturity_date       DATE                NOT NULL,
    status              ENUM('ACTIVE', 'MATURED', 'SETTLED') NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version             INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_portfolio_positions_allocation (allocation_id),
    KEY idx_portfolio_positions_investor_status (investor_id, status, maturity_date),
    KEY idx_portfolio_positions_opportunity (opportunity_id),
    CONSTRAINT fk_portfolio_positions_investor
        FOREIGN KEY (investor_id) REFERENCES users (id),
    CONSTRAINT fk_portfolio_positions_opportunity
        FOREIGN KEY (opportunity_id) REFERENCES opportunities (id),
    CONSTRAINT fk_portfolio_positions_allocation
        FOREIGN KEY (allocation_id) REFERENCES allocations (id),
    CONSTRAINT chk_portfolio_invested_amount_positive
        CHECK (invested_amount > 0),
    CONSTRAINT chk_portfolio_expected_return_positive
        CHECK (expected_return > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- audit_logs (append-only)
-- Written in the same transaction as the business action it records.
-- ---------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    actor_id        BIGINT UNSIGNED     NULL,
    actor_role      ENUM('ADMIN', 'INVESTOR', 'ISSUER', 'SYSTEM') NOT NULL,
    action          VARCHAR(100)        NOT NULL,
    entity_type     VARCHAR(50)         NOT NULL,
    entity_id       BIGINT UNSIGNED     NOT NULL,
    before_state    JSON                NULL,
    after_state     JSON                NULL,
    correlation_id  VARCHAR(64)         NULL,
    created_at      TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_audit_logs_entity_timeline (entity_type, entity_id, created_at),
    KEY idx_audit_logs_actor_created (actor_id, created_at),
    KEY idx_audit_logs_action_created (action, created_at),
    CONSTRAINT fk_audit_logs_actor
        FOREIGN KEY (actor_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- outbox_events (transactional outbox)
-- Inserted in the same transaction as allocation / settlement actions.
-- Poller reads PENDING rows ordered by created_at.
-- ---------------------------------------------------------------------------
CREATE TABLE outbox_events (
    id              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    event_type      ENUM(
                        'INVESTMENT_SUCCESSFUL',
                        'MATURITY_DUE',
                        'SETTLEMENT_COMPLETE'
                    )                   NOT NULL,
    aggregate_type  VARCHAR(50)         NOT NULL,
    aggregate_id    BIGINT UNSIGNED     NOT NULL,
    payload         JSON                NOT NULL,
    status          ENUM('PENDING', 'PUBLISHED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    retry_count     INT UNSIGNED        NOT NULL DEFAULT 0,
    last_error      VARCHAR(1000)       NULL,
    created_at      TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at    TIMESTAMP(6)        NULL,
    PRIMARY KEY (id),
    KEY idx_outbox_events_poller (status, created_at),
    KEY idx_outbox_events_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
