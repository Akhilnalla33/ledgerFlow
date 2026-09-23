-- Accounts hold a cached balance for fast reads. The cached balance is written in the same
-- transaction as the ledger entries that justify it, and is never the source of truth on its
-- own: GET /accounts/{id}/reconcile recomputes it from ledger_entries and flags drift.
CREATE TABLE accounts (
    id              UUID PRIMARY KEY,
    owner_id        VARCHAR(64)     NOT NULL,
    currency        VARCHAR(3)      NOT NULL,
    balance         NUMERIC(19, 4)  NOT NULL DEFAULT 0,
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_owner_id ON accounts (owner_id);

-- One transfer = exactly two balanced ledger entries (one DEBIT, one CREDIT) sharing
-- transfer_id, sum(amount signed) == 0. This table is append-only: application code never
-- issues UPDATE or DELETE against it, and the trigger below enforces that at the DB level.
CREATE TABLE ledger_entries (
    id              UUID PRIMARY KEY,
    account_id      UUID            NOT NULL REFERENCES accounts (id),
    transfer_id     UUID            NOT NULL,
    entry_type      VARCHAR(6)      NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount          NUMERIC(19, 4)  NOT NULL CHECK (amount > 0),
    currency        VARCHAR(3)      NOT NULL,
    balance_after   NUMERIC(19, 4)  NOT NULL,
    description     VARCHAR(256),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_ledger_entries_account_id ON ledger_entries (account_id, created_at);
CREATE INDEX idx_ledger_entries_transfer_id ON ledger_entries (transfer_id);

CREATE OR REPLACE FUNCTION reject_ledger_entry_mutation() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'ledger_entries is append-only: % is not permitted', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ledger_entries_no_update
    BEFORE UPDATE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION reject_ledger_entry_mutation();

CREATE TRIGGER trg_ledger_entries_no_delete
    BEFORE DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION reject_ledger_entry_mutation();

-- One row per initiated transfer, primarily so the API can report status; the ledger entries
-- above remain the source of truth for money movement.
CREATE TABLE transfers (
    id                  UUID            PRIMARY KEY,
    from_account_id     UUID            NOT NULL REFERENCES accounts (id),
    to_account_id       UUID            NOT NULL REFERENCES accounts (id),
    amount              NUMERIC(19, 4)  NOT NULL CHECK (amount > 0),
    currency            VARCHAR(3)      NOT NULL,
    status              VARCHAR(16)     NOT NULL,
    reason              VARCHAR(256),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- Transactional outbox: written in the same DB transaction as the balance/ledger mutation,
-- relayed to Kafka by a separate poller. Guarantees "state changed" and "event published"
-- never diverge.
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

-- Idempotency: the (idempotency_key) is unique per mutating endpoint call. request_hash
-- detects a client reusing a key with a different payload (a client bug, not a legitimate
-- replay), and response_* lets a genuine replay return the original result without
-- re-executing the transfer.
CREATE TABLE idempotency_keys (
    idempotency_key     VARCHAR(128)    PRIMARY KEY,
    request_hash        VARCHAR(64)     NOT NULL,
    response_status     INT,
    response_body       TEXT,
    in_progress         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);
