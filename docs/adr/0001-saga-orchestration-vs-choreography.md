# ADR-0001: Orchestrated saga, not choreography, for settlements

## Status
Accepted

## Context
A settlement splits money from one payer across N payees. That is N+1 related state changes
(the "logical" settlement, plus one transfer per payee) that must either all happen or be
fully undone — a textbook saga. There are two standard ways to implement a saga:

- **Choreography**: each service reacts to events published by the previous one. account-service
  would publish "transfer completed/failed" events, and something downstream would decide what
  to do next, purely from consuming the event stream.
- **Orchestration**: a dedicated coordinator (settlement-service) explicitly calls each
  participant, tracks progress, and decides on success or compensation.

## Decision
settlement-service **orchestrates** explicitly: `SettlementOrchestrator` calls account-service's
transfer endpoint synchronously, one step at a time, and drives the saga state machine
(`STARTED -> IN_PROGRESS -> COMPLETED` or `-> COMPENSATING -> COMPENSATED`) itself.

## Why
- **The "what happens on failure" logic has to live somewhere as one readable piece.** With
  choreography, "debit the payer, and if payee 3 of 4 fails, reverse payees 1 and 2" turns into
  a set of event handlers scattered across services, each reacting to a slice of the picture. In
  an orchestrated saga it is one method (`SettlementOrchestrator.execute`) that a reviewer — or
  an on-call engineer at 2am — can read top to bottom.
- **A settlement genuinely needs a synchronous answer per step.** Whether to proceed to payee 2
  depends on whether payee 1's debit succeeded *right now*, not "eventually, once an event
  arrives." That is a natural fit for synchronous REST calls between the orchestrator and
  account-service, not a Kafka round-trip per step.
- **Compensation order matters and needs a single source of truth for "what already
  succeeded."** The orchestrator's own step list (persisted via `SettlementPersister`) is that
  source of truth; a choreographed design would need to reconstruct it from a stream of events
  from multiple services.
- **Explicit orchestration is easier to test.** `SettlementCompensationTest` mocks exactly one
  collaborator (`AccountServiceClient`) and asserts the resulting state machine — no event bus,
  no multi-service integration test needed to prove compensation works.

## Trade-offs accepted
- settlement-service is a single point of coordination — if it crashes mid-saga, no other
  service will pick up where it left off. Every step's outcome is persisted immediately
  (`SettlementPersister`, each transition its own committed transaction), so the saga's state is
  never lost, but resuming an interrupted saga automatically is not implemented in this project.
  At 10x scale this would need a saga recovery job that scans for `IN_PROGRESS` settlements
  older than a timeout and resumes or compensates them.
- account-service doesn't know it's part of a saga — it just executes ordinary, idempotent
  transfers. This is intentional: the saga is a property of settlement-service's orchestration,
  not something account-service needs to be aware of.
