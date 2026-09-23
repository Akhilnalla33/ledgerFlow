#!/bin/bash
# Fires a real settlement across several accounts against a running `docker compose up` stack
# and prints the before/after ledger state, so correctness can be seen without reading code.
set -euo pipefail

ACCOUNT_URL=${ACCOUNT_URL:-http://localhost:8081}
SETTLEMENT_URL=${SETTLEMENT_URL:-http://localhost:8082}
KEYCLOAK_URL=${KEYCLOAK_URL:-http://localhost:8180}

echo "==> Fetching an access token from Keycloak (realm: ledgerflow, user: demo)"
TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/ledgerflow/protocol/openid-connect/token" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=ledgerflow-demo&username=demo&password=demo' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])')
AUTH=(-H "Authorization: Bearer $TOKEN")

create_account() {
  local owner=$1
  curl -s "${AUTH[@]}" -H 'Content-Type: application/json' \
    -d "{\"ownerId\":\"$owner\",\"currency\":\"USD\"}" \
    "$ACCOUNT_URL/api/v1/accounts" | python3 -c 'import sys,json; print(json.load(sys.stdin)["id"])'
}

fund_account() {
  local funding_id=$1 to=$2 amount=$3 key=$4
  curl -s "${AUTH[@]}" -H 'Content-Type: application/json' -H "Idempotency-Key: $key" \
    -d "{\"fromAccountId\":\"$funding_id\",\"toAccountId\":\"$to\",\"amount\":\"$amount\",\"currency\":\"USD\",\"reason\":\"seed\"}" \
    "$ACCOUNT_URL/api/v1/transfers" > /dev/null
}

balance() {
  curl -s "${AUTH[@]}" "$ACCOUNT_URL/api/v1/accounts/$1" | python3 -c 'import sys,json; print(json.load(sys.stdin)["balance"])'
}

echo "==> Creating accounts: payer, and 3 payees for a dinner split"
FUNDING=$(create_account "system-funding")
PAYER=$(create_account "alice")
P1=$(create_account "bob")
P2=$(create_account "carol")
P3=$(create_account "dave")

echo "==> Seeding payer with 300.00 USD"
fund_account "$FUNDING" "$PAYER" "300.00" "demo-seed-$(date +%s)"

echo
echo "==> BEFORE settlement"
echo "  alice (payer): $(balance "$PAYER")"
echo "  bob   (payee): $(balance "$P1")"
echo "  carol (payee): $(balance "$P2")"
echo "  dave  (payee): $(balance "$P3")"

echo
echo "==> Splitting a 90.00 dinner bill 3 ways (30.00 each) via settlement-service"
curl -s "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"payerAccountId\":\"$PAYER\",\"currency\":\"USD\",\"reason\":\"dinner split\",\"payees\":[{\"payeeAccountId\":\"$P1\",\"amount\":\"30.00\"},{\"payeeAccountId\":\"$P2\",\"amount\":\"30.00\"},{\"payeeAccountId\":\"$P3\",\"amount\":\"30.00\"}]}" \
  "$SETTLEMENT_URL/api/v1/settlements" | python3 -m json.tool

echo
echo "==> AFTER settlement"
echo "  alice (payer): $(balance "$PAYER")"
echo "  bob   (payee): $(balance "$P1")"
echo "  carol (payee): $(balance "$P2")"
echo "  dave  (payee): $(balance "$P3")"

echo
echo "==> Reconciliation check on the payer account"
curl -s "${AUTH[@]}" "$ACCOUNT_URL/api/v1/accounts/$PAYER/reconcile" | python3 -m json.tool
