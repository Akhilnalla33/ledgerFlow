-- Read model, kept eventually consistent by consuming account-service's
-- AccountBalanceChanged events off Kafka. Never written to by any synchronous request path.
CREATE TABLE balance_snapshots (
    account_id      UUID            PRIMARY KEY,
    balance         NUMERIC(19, 4)  NOT NULL,
    currency        VARCHAR(3)      NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL
);

CREATE TABLE statement_entries (
    id                  UUID            PRIMARY KEY,
    account_id          UUID            NOT NULL,
    transfer_id         UUID            NOT NULL,
    direction           VARCHAR(6)      NOT NULL,
    amount              NUMERIC(19, 4)  NOT NULL,
    currency            VARCHAR(3)      NOT NULL,
    resulting_balance   NUMERIC(19, 4)  NOT NULL,
    reason              VARCHAR(256),
    occurred_at         TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_statement_entries_account ON statement_entries (account_id, occurred_at DESC);

-- Audit trail of settlement saga status transitions, consumed from settlement-service's
-- SettlementStatusChanged events.
CREATE TABLE settlement_audit_entries (
    id                  UUID            PRIMARY KEY,
    settlement_id       UUID            NOT NULL,
    status              VARCHAR(16)     NOT NULL,
    failure_reason      VARCHAR(512),
    occurred_at         TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_settlement_audit_settlement ON settlement_audit_entries (settlement_id, occurred_at);
