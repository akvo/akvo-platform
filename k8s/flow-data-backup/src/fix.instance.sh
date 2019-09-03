# Script add/update any setup needed for the backup, as some old instances are setup differently from the current ones.
# This script is not part of the cron job because it needs admin permissions for these operations.
# In theory this script does not need to be run again.
## To run:
##      docker run --rm=true -ti -v `pwd`:/p12 --name gcloud-config2 google/cloud-sdk:258.0.0
## Make sure that the akvo-flow-server-config is checkout at the parent directory.
## Then login with the admin account (or any other account that have enough permissions):
##      gcloud auth login
#!/usr/bin/env bash

instance=$1

gcloud config set project $instance
## This API is required to enable other APIs
gcloud services enable serviceusage.googleapis.com

account=$(cat ../${instance}/appengine-web.xml | grep serviceAccountId | grep -o "value=.*" | cut -f 2 -d\")

## Adding new required permission to the service account
gcloud projects add-iam-policy-binding ${instance} --member=serviceAccount:${account} --role=roles/datastore.importExportAdmin
## The default services accounts in old flow instances did not have editor permission
gcloud projects add-iam-policy-binding ${instance} --member=serviceAccount:${instance}@appspot.gserviceaccount.com --role=roles/editor
