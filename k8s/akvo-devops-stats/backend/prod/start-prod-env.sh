#!/usr/bin/env bash

set -e

_term() {
  echo "Caught SIGTERM signal!"
  kill -TERM "$child" 2>/dev/null
}

trap _term SIGTERM

java -jar akvo-devops-stats.jar &

child=$!
wait "$child"