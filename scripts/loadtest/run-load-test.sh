#!/bin/bash
# Reproduces the load test reported in the README. Runs account-service locally with an
# in-memory H2 database and permissive local-only security (see LoadTestSecurityConfig) so the
# test doesn't require Postgres, Kafka, or Keycloak to be running -- it is purely a load test of
# the transfer endpoint's concurrency correctness and latency, not a full-stack test.
set -euo pipefail
cd "$(dirname "$0")/../.."

NUM_ACCOUNTS=${NUM_ACCOUNTS:-50}

echo "==> Building account-service"
(cd account-service && mvn -q -DskipTests package)

echo "==> Starting account-service with the loadtest profile"
(cd account-service && mvn -q spring-boot:run \
  -Dspring-boot.run.profiles=loadtest \
  -Dspring-boot.run.useTestClasspath=true > /tmp/ledgerflow-loadtest-account-service.log 2>&1 &)

echo "==> Waiting for account-service to come up on :8081"
for i in $(seq 1 30); do
  if curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/health | grep -q 200; then
    echo "    up"
    break
  fi
  sleep 2
done

echo "==> Seeding $NUM_ACCOUNTS funded accounts"
python3 scripts/loadtest/seed_loadtest.py "$NUM_ACCOUNTS"

echo "==> Running k6 (30s, 40 VUs unique transfers + 10 VUs idempotent replays)"
BASE_URL=http://localhost:8081 k6 run scripts/loadtest/transfer-load-test.js

echo "==> Post-load-test balance-correctness check (global double-entry invariant)"
curl -s http://localhost:8081/api/v1/ledger/reconciliation | python3 -m json.tool

echo "==> Stopping account-service"
pkill -f "spring-boot:run" || true
