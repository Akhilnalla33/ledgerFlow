# ADR-0003: Polling outbox table, not Debezium/CDC, for reliable event publishing

## Status
Accepted

## Context
account-service and settlement-service must publish a Kafka event for every state change
(balance change, settlement status change) without ever losing an event or publishing one that
doesn't correspond to a real, committed state change. Writing to Postgres and publishing to
Kafka are two separate systems with no shared transaction, so the naive approach — write to the
DB, then publish to Kafka in the same request — has a dual-write bug: the process can crash (or
Kafka can be unreachable) between the two writes, leaving the DB changed but no event published,
or (with `@TransactionalEventListener(phase = AFTER_COMMIT)`) an event published for a commit
that a later step in the same logical operation still fails.

The transactional outbox pattern fixes this: the state change and an `outbox_events` row are
written in the *same* local database transaction, so they are atomic by construction. Something
else then relays outbox rows to Kafka. Two ways to build that "something else":

- **CDC (e.g. Debezium)**: tail the database's write-ahead log and stream outbox inserts to
  Kafka automatically, with no polling and near-zero latency.
- **A polling publisher**: an in-process scheduled job periodically selects PENDING rows,
  publishes them, and marks them PUBLISHED.

## Decision
`OutboxPublisher` (in both account-service and settlement-service) is a `@Scheduled` poller
that runs every 500ms, selects a batch of PENDING rows with `SELECT ... FOR UPDATE`, publishes
each to Kafka via `KafkaTemplate`, and marks it PUBLISHED — or leaves it PENDING (incrementing a
retry count) if the publish fails.

## Why
- **No extra infrastructure.** Debezium needs a Kafka Connect cluster, WAL-level access
  configured on Postgres (`wal_level = logical`, a replication slot per service), and its own
  operational surface (connector configs, offset tracking, schema registry integration). For a
  project at this scale, a poller is a single `@Component` with no additional moving parts —
  and it is honest about that trade-off rather than hand-waving a CDC pipeline that was never
  actually stood up and tested.
- **The correctness guarantee is identical.** Both approaches rely on the same underlying fact:
  the outbox row is committed atomically with the state change. Whether it's *read* by a WAL
  tailer or a polling `SELECT` doesn't change that guarantee — it only changes latency and
  operational complexity.
- **Explicit, debuggable retry semantics.** `OutboxEvent.retryCount` and the FAILED status (after
  10 attempts) are visible directly in the `outbox_events` table and via
  `/actuator/metrics` — an operator can `SELECT * FROM outbox_events WHERE status = 'FAILED'`
  and understand exactly what didn't get published and why, without needing Kafka Connect's own
  dead-letter/error-handling configuration.

## Trade-offs accepted
- **Latency**: events lag their state change by up to the poll interval (500ms here), not
  near-real-time. For LedgerFlow's use case (balance projections, notifications) that's fine;
  it would not be fine for, say, a sub-100ms fraud-scoring pipeline.
- **Polling load**: every poll issues a `SELECT ... FOR UPDATE` even when there's nothing
  pending. At low volume this is negligible; at high volume it's extra database load that CDC
  avoids entirely (CDC only does work when there's something to stream).
- **What I'd do differently at 10x scale**: move to Debezium once the operational cost is
  justified by either the latency requirement or the polling load becoming measurable — the
  outbox table itself doesn't change, only how it's drained, so this migration doesn't require
  touching the write path at all.
