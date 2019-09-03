#!/usr/bin/env bash
# Script to restore a backup.
## To run, make sure that the akvo-flow-server-config is checkout at the parent directory.
##      docker run --rm=true -ti -v `pwd`/../akvo-flow-server-config:/p12 -v `pwd`:/work --name gcloud-config2 google/cloud-sdk:258.0.0
# cd /work
# bash restore.sh akvoflow-dev1 gs://akvobackupakvoflow-dev1/testing/2019-09-03T10:36:20/2019-09-03T10:36:20.overall_export_metadata

set -eu

function log {
   echo "$(date +"%T") - INFO - $*"
}

instance=$1
p12_file=/p12/${instance}/${instance}.p12
bucket_path=$2

if ! dpkg -s python-openssl > /dev/null; then
    apt-get update && apt-get -qq install -y --no-install-recommends --no-install-suggests python-openssl python3-openssl python3-pip
    pip3 install google-cloud-datastore
fi

export CLOUDSDK_PYTHON_SITEPACKAGES=1

account=$(cat /p12/${instance}/appengine-web.xml | grep serviceAccountId | grep -o "value=.*" | cut -f 2 -d\")

log Account for ${instance} is --${account}--
gcloud auth activate-service-account ${account} --key-file=${p12_file}  --project ${instance}

if ! gsutil ls ${bucket_path}; then
    log "Bucket does not exists. Cannot restore anything"
    exit 1
fi

gcloud datastore import ${bucket_path}