#!/usr/bin/env bash

set -eu

MAX_ATTEMPTS=120
ATTEMPTS=0

echo "Waiting for PostgreSQL ..."

ATTEMPTS=0
PG=""
SQL="SELECT 1"

while [[ -z "${PG}" && "${ATTEMPTS}" -lt "${MAX_ATTEMPTS}" ]]; do
    export PGPASSWORD=xxxxpasswd
    PG=$( (psql --username=devopsstatsdbuser --host=postgres --dbname=devopsstatsdb --command "${SQL}" 2>&1 | grep "(1 row)") || echo "")
    let ATTEMPTS+=1
    sleep 1
done

if [[ -z "${PG}" ]]; then
    echo "PostgreSQL is not available"
    exit 1
fi

echo "PostgreSQL is ready!"
