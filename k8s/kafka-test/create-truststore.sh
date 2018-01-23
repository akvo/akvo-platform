#!/usr/bin/env bash

set -e

private_key_password=<<your password here>>
truststore_password=<<your password here>>

VALIDITY_IN_DAYS=3650
DEFAULT_TRUSTSTORE_FILENAME="kafka.truststore.jks"
TRUSTSTORE_WORKING_DIRECTORY="truststore"
CA_CERT_FILE="ca-cert"

function file_exists_and_exit() {
  echo "'$1' cannot exist. Move or delete it before"
  echo "re-running this script."
  exit 1
}

if [ -e "$TRUSTSTORE_WORKING_DIRECTORY" ]; then
  file_exists_and_exit $TRUSTSTORE_WORKING_DIRECTORY
fi

mkdir $TRUSTSTORE_WORKING_DIRECTORY
echo
echo "OK, we'll generate a trust store and associated private key."
echo
echo "First, the private key."
echo
echo "You will be prompted for:"
echo " - A password for the private key. Remember this."
echo " - Information about you and your company."
echo " - NOTE that the Common Name (CN) is currently not important."

openssl req -new -x509 -keyout $TRUSTSTORE_WORKING_DIRECTORY/ca-key \
  -subj "/C=ES/ST=Akvo/L=Akvo/O=Akvo/OU=Akvo/CN=AkvoTest" \
  -passout pass:${private_key_password}  \
  -out $TRUSTSTORE_WORKING_DIRECTORY/ca-cert -days $VALIDITY_IN_DAYS

trust_store_private_key_file="$TRUSTSTORE_WORKING_DIRECTORY/ca-key"

echo
echo "Two files were created:"
echo " - $TRUSTSTORE_WORKING_DIRECTORY/ca-key -- the private key used later to"
echo "   sign certificates"
echo " - $TRUSTSTORE_WORKING_DIRECTORY/ca-cert -- the certificate that will be"
echo "   stored in the trust store in a moment and serve as the certificate"
echo "   authority (CA). Once this certificate has been stored in the trust"
echo "   store, it will be deleted. It can be retrieved from the trust store via:"
echo "   $ keytool -keystore <trust-store-file> -export -alias CARoot -rfc"

echo
echo "Now the trust store will be generated from the certificate."
echo
echo "You will be prompted for:"
echo " - the trust store's password (labeled 'keystore'). Remember this"
echo " - a confirmation that you want to import the certificate"

keytool -keystore $TRUSTSTORE_WORKING_DIRECTORY/$DEFAULT_TRUSTSTORE_FILENAME \
-noprompt -storepass ${truststore_password} \
-alias CARoot -import -file $TRUSTSTORE_WORKING_DIRECTORY/ca-cert

trust_store_file="$TRUSTSTORE_WORKING_DIRECTORY/$DEFAULT_TRUSTSTORE_FILENAME"

echo
echo "$TRUSTSTORE_WORKING_DIRECTORY/$DEFAULT_TRUSTSTORE_FILENAME was created."

# don't need the cert because it's in the trust store.
rm $TRUSTSTORE_WORKING_DIRECTORY/$CA_CERT_FILE