#!/usr/bin/env bash

eval "$(sentry-cli bash-hook)"

function log {
   echo "$(date +"%T") - INFO - $*"
}

instance=$1
## We are adding the instance as a env var so that sentry-cli adds it to the failure message, so that we know which instance failed.
export FLOW_INSTANCE=${instance}
instance_config_directory=$2
bucket=gs://akvobackup-${instance}
bucket_path=${bucket}/$(date "+%FT%T")
p12_file=${instance_config_directory}/${instance}.p12

export CLOUDSDK_PYTHON_SITEPACKAGES=1

account=$(cat ${instance_config_directory}/appengine-web.xml | grep serviceAccountId | grep -o "value=.*" | cut -f 2 -d\")

log Account for ${instance} is --${account}--
gcloud auth activate-service-account ${account} --key-file=${p12_file}  --project ${instance}

if ! gcloud services list | grep appengine; then
    log "Enabling API appengine.googleapis.com. This sometimes takes a bit of time which causes the bucket creation to not work"
    gcloud services enable appengine.googleapis.com
fi

if ! gcloud services list | grep storage-api; then
    log "Enabling API storage-api.googleapis.com. This sometimes takes a bit of time which causes the bucket creation to not work"
    gcloud services enable storage-api.googleapis.com
fi

if ! gcloud services list | grep storage-component; then
    log "Enabling API storage-component.googleapis.com. This sometimes takes a bit of time which causes the bucket creation to not work"
    gcloud services enable storage-component.googleapis.com
fi

if ! gsutil ls ${bucket}; then
    project_location=$(gcloud app describe | grep locationId | cut -f2 -d\ )
    region=$(echo ${project_location} | cut -b1-2)
    if [[ -z "${region}" ]]; then
        log "Cannot determine the region for the bucket given the location --$project_location--"
        exit 1
    fi
    log "Creating bucket ${bucket} in region ${region} (from location ${project_location})"
    gsutil mb -l ${region} -c coldline --retention 90d ${bucket}
fi

python3 convert.py ${instance} ${p12_file} ${account} service-account.json
export GOOGLE_APPLICATION_CREDENTIALS=service-account.json
kinds=$(python3 kinds.py ${instance})

log backing up ${instance} to ${bucket_path}, kinds: ${kinds}
gcloud datastore export --namespaces="(default)" --kinds=${kinds} ${bucket_path}