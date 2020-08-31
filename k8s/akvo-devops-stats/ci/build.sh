#!/usr/bin/env bash

set -eu

function log {
   echo "$(date +"%T") - INFO - $*"
}

export PROJECT_NAME=akvo-lumen

if [ -z "${TRAVIS_COMMIT:-}" ]; then
    export TRAVIS_COMMIT=local
fi

log Building backend dev container
docker build --rm=false -t akvo-devops-dev:develop backend -f backend/dev/Dockerfile-dev
log Building uberjar
docker run -v $HOME/.m2:/root/.m2 -v $(pwd)/backend:/app akvo-devopsstats-dev:develop lein uberjar

log Building production container
docker build --rm=false -t eu.gcr.io/${PROJECT_NAME}/akvo-devopsstats:$TRAVIS_COMMIT backend -f backend/prod/Dockerfile