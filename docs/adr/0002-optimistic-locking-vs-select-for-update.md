# ADR-0002: Optimistic locking (`@Version`), not `SELECT ... FOR UPDATE`, on account balances

## Status
Accepted

## Context
Every transfer mutates two `accounts` rows (debit one, credit the other). Under concurrent
transfers against the same account, naive read-modify-write is a classic lost-update bug:
thread A reads balance=100, thread B reads balance=100, A writes 90, B writes 80 — A's debit
is silently overwritten. Two standard fixes exist:

- **Pessimistic locking** (`SELECT ... FOR UPDATE`): the first transaction to touch a row locks
  it; every other transaction touching the same row blocks until the lock is released.
- **Optimistic locking** (a `@Version` column): every read captures a version number; the
  UPDATE statement includes `WHERE version = <the version that was read>`; if another
  transaction already bumped it, zero rows are affected, Hibernate raises
  `ObjectOptimisticLockingFailureException`, and the caller retries against the fresh row.

## Decision
`Account.version` is a JPA `@Version` column. `TransferService.executeWithRetry` catches
`ObjectOptimisticLockingFailureException` and retries the whole attempt (reloading both
accounts, re-applying debit/credit, re-writing ledger entries) up to 10 times.

## Why
- **Transfers are typically fast and contention on any one account is bursty, not sustained.**
  Optimistic locking has zero cost in the (overwhelmingly common) uncontended case — no lock is
  ever held — and only pays a retry cost when two transfers genuinely race on the same account
  at the same instant. `SELECT ... FOR UPDATE` pays a lock-acquisition cost on every single
  transfer, contended or not.
- **No deadlock risk between the two accounts a transfer touches.** A transfer locks two rows
  (debited and credited account). With `FOR UPDATE`, two transfers moving money in opposite
  directions between the same pair of accounts can deadlock unless every caller is disciplined
  about lock acquisition order. `LedgerService.executeTransfer` still sorts by account id before
  loading (defense in depth, and it makes profiling/debugging saner), but with optimistic
  locking a genuine conflict resolves itself via a clean exception and retry rather than the
  database's deadlock detector killing a transaction.
- **This is the property `ConcurrentTransferCorrectnessTest` exists to prove.** That test fires
  32 threads x 25 transfers each at a shared hub account and asserts the final balance is exact
  to the cent — the retry-on-conflict loop is what makes that true under real concurrent load,
  not just in the single-threaded case.

## Trade-offs accepted
- Under very high contention on one specific "hot" account (e.g. a single popular merchant
  account receiving thousands of concurrent payments per second), optimistic locking's retry
  storm could actually perform worse than a well-tuned pessimistic lock queue, because every
  losing thread redoes its full read-modify-write cycle instead of just waiting once. At 10x
  scale, a hot-account problem like that would be better solved by sharding the account's
  balance into partial balances that are summed on read (or moving to an event-sourced ledger
  where "transfer" is just an append, with balance computed asynchronously) rather than by
  switching locking strategies.
- The retry loop bounds itself at 10 attempts and then surfaces the conflict as an HTTP 409 —
  under pathological contention a client could see a definitive failure rather than an
  eventually-successful retry. This is treated as acceptable backpressure, not a bug.
