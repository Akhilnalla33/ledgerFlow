# settlement-service

Orchestrates multi-account settlement sagas (e.g. "split this bill N ways"), calling
account-service synchronously per step and compensating already-completed steps if a later
step fails. See [ADR-0001](../docs/adr/0001-saga-orchestration-vs-choreography.md) and the
[root README](../README.md).

## Endpoints

| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/settlements` | Creates and synchronously executes a settlement |
| GET | `/api/v1/settlements/{id}` | Get settlement + step status |

Swagger UI: `http://localhost:8082/swagger-ui.html` when running.

## Running locally

```bash
mvn -pl settlement-service -am spring-boot:run
```
Needs account-service reachable at `ledgerflow.account-service.base-url` (defaults to
`http://localhost:8081`).

## Tests

```bash
mvn -pl settlement-service -am verify
```

`SettlementCompensationTest` is the saga-compensation centerpiece: mocks account-service and
proves a mid-saga failure compensates every already-completed step. Testcontainers-backed
`*IT.java` tests are gated behind `-Pdocker-integration-tests`.
