#!/usr/bin/env bash

eval "$(sentry-cli bash-hook)"

FLOW_CONFIG_DIRECTORY=flow-config
mkdir ${FLOW_CONFIG_DIRECTORY}
curl --header "Authorization: token ${GITHUB_AUTH_TOKEN}" -L https://api.github.com/repos/akvo/akvo-flow-server-config/tarball/master > flow-config.tar.gz
tar xzvf flow-config.tar.gz --strip-components=1 --directory=${FLOW_CONFIG_DIRECTORY}

for i in `find ${FLOW_CONFIG_DIRECTORY} | grep p12 | cut -f 2 -d/ | sort`; do
    echo "Starting backup for $i"
#    ./backup.sh ${i} ${FLOW_CONFIG_DIRECTORY}/${i} || echo "Backup failed for $i"
done

i=akvoflow-dev2
./backup.sh ${i} ${FLOW_CONFIG_DIRECTORY}/${i} || echo "Backup failed for $i"
