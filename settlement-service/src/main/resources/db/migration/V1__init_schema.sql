CREATE TABLE settlements (
    id              UUID            PRIMARY KEY,
    payer_account_id UUID           NOT NULL,
    currency        VARCHAR(3)      NOT NULL,
    total_amount    NUMERIC(19, 4)  NOT NULL,
    status          VARCHAR(16)     NOT NULL,
    failure_reason  VARCHAR(512),
    reason          VARCHAR(256),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- One row per payee share. step_order defines both the execution order of debits during the
-- forward saga and the reverse order in which compensations are issued on failure.
CREATE TABLE settlement_steps (
    id              UUID            PRIMARY KEY,
    settlement_id   UUID            NOT NULL REFERENCES settlements (id),
    step_order      INT             NOT NULL,
    payee_account_id UUID           NOT NULL,
    amount          NUMERIC(19, 4)  NOT NULL CHECK (amount > 0),
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    transfer_id     UUID,
    compensation_transfer_id UUID,
    failure_reason  VARCHAR(512),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_settlement_steps_settlement_id ON settlement_steps (settlement_id, step_order);

CREATE TABLE outbox_events (
    id              UUID            PRIMARY KEY,
    aggregate_type  VARCHAR(32)     NOT NULL,
    aggregate_id    UUID            NOT NULL,
    event_type      VARCHAR(64)     NOT NULL,
    topic           VARCHAR(128)    NOT NULL,
    payload         TEXT            NOT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    retry_count     INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ
);

CREATE INDEX idx_outbox_status_created ON outbox_events (status, created_at);
