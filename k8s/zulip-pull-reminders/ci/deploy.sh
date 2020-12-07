#!/usr/bin/env bash

set -eu

function log {
   echo "$(date +"%T") - INFO - $*"
}

export PROJECT_NAME=akvo-lumen

if [[ "${TRAVIS_BRANCH:-}" != "master" ]]; then
    exit 0
fi

if [[ "${TRAVIS_PULL_REQUEST:-}" != "false" ]]; then
    exit 0
fi

log Making sure gcloud and kubectl are installed and up to date
gcloud components install kubectl
gcloud components update
gcloud version

log Authentication with gcloud and kubectl
gcloud config set project akvo-lumen
gcloud config set container/cluster europe-west1-d
gcloud config set compute/zone europe-west1-d
gcloud config set container/use_client_certificate True

log Environment is test
gcloud container clusters get-credentials test

log Pushing images
gcloud auth configure-docker
docker push eu.gcr.io/${PROJECT_NAME}/zulip-pull-reminders

sed -e "s/\${TRAVIS_COMMIT}/${TRAVIS_COMMIT:-local}/" ci/k8s/cronjob.yaml.template > cronjob.yaml.donotcommit

kubectl apply -f cronjob.yaml.donotcommit
