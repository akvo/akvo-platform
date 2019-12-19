#!/usr/bin/env bash

set -u

eval "$(sentry-cli bash-hook)"

FLOW_CONFIG_DIRECTORY=flow-config
mkdir "${FLOW_CONFIG_DIRECTORY}"
curl --header "Authorization: token ${GITHUB_AUTH_TOKEN}" \
     --location "https://api.github.com/repos/akvo/akvo-flow-server-config/tarball/master" > flow-config.tar.gz
tar xzvf flow-config.tar.gz --strip-components=1 --directory="${FLOW_CONFIG_DIRECTORY}"

for i in $(find "${FLOW_CONFIG_DIRECTORY}" -type f -name '*.p12' | cut -f 2 -d/ | sort); do

    if [[ -f "${FLOW_CONFIG_DIRECTORY}/${i}/.skip-backup" ]]; then
	echo "Skipping backup for ${i}"
	continue
    fi

    echo "Starting backup for ${i}"
    ./backup.sh "${i}" "${FLOW_CONFIG_DIRECTORY}/${i}" || echo "Backup failed for ${i}"
done
