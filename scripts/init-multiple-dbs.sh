#!/bin/bash
# Runs once, on first boot of the shared Postgres container, via docker-entrypoint-initdb.d.
# Each LedgerFlow service gets its own database (never a shared schema) even though they all
# happen to run on one Postgres instance in this compose stack for simplicity.
set -euo pipefail

for db in account_db settlement_db ledger_query_db; do
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    SELECT 'CREATE DATABASE $db OWNER $POSTGRES_USER'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
EOSQL
done
