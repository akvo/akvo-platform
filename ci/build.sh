#!/usr/bin/env bash

set -e

BRANCH_NAME="${TRAVIS_BRANCH:=unknown}"

if [ -z "$TRAVIS_COMMIT" ]; then
    export TRAVIS_COMMIT=local
fi

cd k8s/keycloak-ha-mysql

TAG=$(echo -n "${TRAVIS_COMMIT}" | cut -c-8)
IMAGE_NAME="akvo/keycloak-ha-mysql:${TAG}"

docker build -t "${DOCKER_IMAGE_NAME:=$IMAGE_NAME}" .
