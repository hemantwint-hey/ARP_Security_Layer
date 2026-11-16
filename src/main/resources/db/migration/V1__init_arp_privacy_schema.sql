-- ============================================================================
-- DATABASE: arp_privacy_db
-- Owned EXCLUSIVELY by arp-privacy-gateway (:8082)
-- Contains PII-linked data (customerId <-> arpToken vault)
-- NEVER shared with spring-ai-advisor-service (:8083)
-- ============================================================================

CREATE TABLE arp_identity_mappings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     BIGINT       NOT NULL,
    user_id         VARCHAR(36)  NOT NULL,
    arp_token       VARCHAR(64)  NOT NULL UNIQUE,
    privacy_mode    VARCHAR(20)  NOT NULL DEFAULT 'BALANCED', -- STRICT | BALANCED
    consent_enabled BOOLEAN      NOT NULL DEFAULT TRUE,
    salt_version    INT          NOT NULL DEFAULT 1,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | ROTATED | REVOKED
    expires_at      DATETIME(6)  NOT NULL,
    revoked_at      DATETIME(6)  NULL,
    created_at      DATETIME(6)  NOT NULL,
    INDEX idx_arp_customer_status (customer_id, status),
    INDEX idx_arp_expires_at (expires_at)
);

CREATE TABLE customer_saving_preferences (
    customer_id               BIGINT         PRIMARY KEY,
    guard_level               VARCHAR(20)    NOT NULL DEFAULT 'OFF', -- OFF | SOFT_GUARD | STRICT_LOCK
    monthly_discretionary_cap DECIMAL(18,2)  NULL,
    single_tx_soft_cap        DECIMAL(18,2)  NULL,
    blocked_categories_json   JSON           NOT NULL,
    fail_closed_on_outage     BOOLEAN        NOT NULL DEFAULT FALSE,
    updated_at                DATETIME(6)    NOT NULL,
    CONSTRAINT chk_pref_monthly_cap CHECK (monthly_discretionary_cap IS NULL OR monthly_discretionary_cap >= 0),
    CONSTRAINT chk_pref_single_cap  CHECK (single_tx_soft_cap IS NULL OR single_tx_soft_cap > 0)
);

CREATE TABLE resolved_customer_insights (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id               BIGINT        NOT NULL,
    source_event_id           VARCHAR(64)   NOT NULL UNIQUE,
    insight_type              VARCHAR(40)   NOT NULL, -- SUBSCRIPTION_LEAK | SPEND_SPIKE | SAVINGS_TIP
    severity                  VARCHAR(16)   NOT NULL, -- INFO | WARN | HIGH
    category                  VARCHAR(40)   NOT NULL,
    title                     VARCHAR(160)  NOT NULL,
    recommendation            TEXT          NOT NULL,
    estimated_monthly_savings DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    acknowledged              BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at                DATETIME(6)   NOT NULL,
    INDEX idx_insight_cust_created (customer_id, created_at DESC)
);

CREATE TABLE saving_guard_metrics (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id      BIGINT        NOT NULL,
    transaction_type VARCHAR(30)   NOT NULL,
    category         VARCHAR(40)   NOT NULL,
    saved_amount     DECIMAL(18,2) NOT NULL,
    action_taken     VARCHAR(30)   NOT NULL, -- BLOCKED | USER_CANCELLED_ON_WARN
    created_at       DATETIME(6)   NOT NULL,
    CONSTRAINT chk_guard_saved_pos CHECK (saved_amount > 0),
    INDEX idx_guard_cust_created (customer_id, created_at DESC)
);

CREATE TABLE privacy_audit_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     BIGINT       NOT NULL,
    arp_token       VARCHAR(64)  NULL,
    audit_action    VARCHAR(40)  NOT NULL, -- TOKEN_MINTED | SANITIZED_FORWARD | ROTATED | REVOKED
    fields_stripped VARCHAR(255) NOT NULL,
    payload_sha256  VARCHAR(64)  NULL,
    created_at      DATETIME(6)  NOT NULL,
    INDEX idx_privacy_audit_cust (customer_id, created_at DESC)
);

CREATE TABLE gateway_processed_events (
    event_id     VARCHAR(64) PRIMARY KEY,
    topic_name   VARCHAR(80) NOT NULL,
    processed_at DATETIME(6) NOT NULL
);

