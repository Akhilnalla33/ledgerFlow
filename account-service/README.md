# account-service

Owns accounts, double-entry ledger entries, transfers, idempotency, optimistic locking, and
the transactional outbox. See the [root README](../README.md) for the full system picture,
architecture diagrams, and how to run everything together.

## Endpoints

| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/accounts` | Create an account |
| GET | `/api/v1/accounts/{id}` | Get an account (owner or ADMIN) |
| GET | `/api/v1/accounts/{id}/ledger-entries` | Paged ledger history |
| GET | `/api/v1/accounts/{id}/reconcile` | Cached balance vs. ledger-derived balance |
| GET | `/api/v1/ledger/reconciliation` | System-wide double-entry check (ADMIN) |
| POST | `/api/v1/transfers` | Requires `Idempotency-Key` header |

Swagger UI: `http://localhost:8081/swagger-ui.html` when running.

## Running locally

```bash
mvn -pl account-service -am spring-boot:run
```
Requires Postgres and Kafka reachable per `application.yml` (or just use the root
`docker compose up`, which wires all of this for you).

## Tests

```bash
mvn -pl account-service -am verify
```

Runs unit tests, the concurrency-correctness test, and the idempotent-replay test against H2
(no Docker needed). Testcontainers-backed `*IT.java` tests are gated behind
`-Pdocker-integration-tests`.
