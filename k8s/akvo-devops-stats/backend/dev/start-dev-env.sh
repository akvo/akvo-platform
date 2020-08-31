#!/usr/bin/env bash

set -e

_term() {
  echo "Caught SIGTERM signal!"
  kill -TERM "$child" 2>/dev/null
}

trap _term SIGTERM

(
./dev/wait-for-dependencies.sh

if [[ "$1" == "integration-test" ]]; then
    lein eftest :integration
else
    lein repl :headless
fi
) &

child=$!
wait "$child"
