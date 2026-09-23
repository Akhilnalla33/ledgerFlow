# ledger-query-service

Read-optimized, CQRS-style projection of account balances, statement history, and settlement
audit trails, built entirely from Kafka events published by account-service and
settlement-service. Never written to by any synchronous request path — see the
[root README](../README.md).

## Endpoints

| Method | Path |
|---|---|
| GET | `/api/v1/accounts/{id}/balance` |
| GET | `/api/v1/accounts/{id}/statement` |
| GET | `/api/v1/settlements/{id}/audit-trail` |

Swagger UI: `http://localhost:8083/swagger-ui.html` when running.

## Running locally

```bash
mvn -pl ledger-query-service -am spring-boot:run
```

## Tests

```bash
mvn -pl ledger-query-service -am verify
```
