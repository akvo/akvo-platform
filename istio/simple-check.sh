#!/usr/bin/env bash

set -u

if [ "$1" == "first" ]; then
shift
echo "using first client"
token=$(curl -s -u akvo-flow-maps-ci-client:$CLIENT_ONE_PASSWORD https://kc.akvotest.org/auth/realms/akvo/protocol/openid-connect/token --data "grant_type=client_credentials" | jq -r ".access_token")
else
echo "using second client"
token=$(curl -s -u akvo-lumen-confidential:$CLIENT_TWO_PASSWORD https://kc.akvotest.org/auth/realms/akvo/protocol/openid-connect/token --data "grant_type=client_credentials" | jq -r ".access_token")
fi

echo -n "With Bearer (expect 200): "
curl -H "Accept: application/vnd.akvo.flow.v2+json" -H "Authorization: Bearer $token"   --verbose   https://dantest.akvotest.org/flow/anything 2>&1 | grep HTTP | grep -v GET | sed "s/.*1\.1 \([^ ]*\) .*/\1/"
echo -n "With Bearer but no accept header (400): "
curl -H "Authorization: Bearer $token"   --verbose   https://dantest.akvotest.org/flow/anything 2>&1 | grep HTTP | grep -v GET | sed "s/.*1\.1 \([^ ]*\) .*/\1/"
echo -n "With invalid bearer (401): "
curl -H "Authorization: Bearer asdfdsfs"   --verbose   https://dantest.akvotest.org/flow/anything 2>&1 | grep HTTP | grep -v GET | sed "s/.*1\.1 \([^ ]*\) .*/\1/"
echo -n "With nothing (401): "
curl -H "Accept: application/vnd.akvo.flow.v2+json" --verbose   https://dantest.akvotest.org/flow/anything 2>&1 | grep HTTP | grep -v GET | sed "s/.*1\.1 \([^ ]*\) .*/\1/"
echo -n "With OPTIONS (200): "
curl -H "Origin: http://example.com"   -H "Access-Control-Request-Method: POST"   -H "Access-Control-Request-Headers: X-Requested-With"   -X OPTIONS --verbose   https://dantest.akvotest.org/flow/anything 2>&1 | grep HTTP | grep -v OPTI | sed "s/.*1\.1 \([^ ]*\) .*/\1/"

if [ "$1" == "429" ]; then
    for i in `seq 12`; do
        curl -H "Accept: application/vnd.akvo.flow.v2+json" -H "Authorization: Bearer $token"   --verbose   https://dantest.akvotest.org/flow/anything >/dev/null 2>&1
    done

    echo -n "After too many request (429): "
    curl -H "Accept: application/vnd.akvo.flow.v2+json" -H "Authorization: Bearer $token"   --verbose   https://dantest.akvotest.org/flow/anything 2>&1 | grep HTTP | grep -v GET | sed "s/.*1\.1 \([^ ]*\) .*/\1/"
fi