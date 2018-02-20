### Kafka + Schema Registry

We are running our own Kafka cluster (of 1 node) in the test cluster.

It is installed in the "kafka" namespace using the Helm chart. 

Note that right now the Helm Tiller is installed in the kubeapps namespace so that kubeapps and Helm share the same Helm releases.

To connect to that Tiller, you will need:

    export TILLER_NAMESPACE=kubeapps

To install Kafka + Schema Registry:

    helm install --namespace kafka --name akvo-test -f kafka.dev.yaml incubator/schema-registry

After that, the Schema Registry must be exposed externally, so you need to change the akvo-test-schema-registry service to "LoadBalancer"

### Rest Proxy

Unfortunately there is no Helm chart for the Rest Proxy, so we install that in the traditional way. In the future we 
should create a Helm chart.

The Rest Proxy authenticates the clients using SSL certificates, that we create. To install it:

1. Download from 1password the Rest Proxy keystore and the Kafka truststore. Put them in the "secrets" folder.
1. Update the keystore,truststore and key passwords in "secrets/kafka-rest.properties". All of them are in 1password.
1. Run from the "secrets" folder:
    ````
    kubectl create --namespace kafka secret generic confluent-secret \
            --from-file=./kafka.keystore.jks \
            --from-file=./kafka.truststore.jks \
            --from-file=./kafka-rest.properties \
            --from-file=./log4j.properties
    ````
    The log4j doesnt really need to be a secret but this way we avoid having a ConfigMap just for it.       
1. Run from this folder:
    ````
    kubectl apply -f rest-service.yml
    kubectl apply -f rest.yml
    ````       

### DNS

You will need to update the DNS records for both the Schema Registry and Rest Proxy

### Certificates

#### Clients

If you need to create new client certificates for applications connecting to Kafka, you will need to:
1. Download from 1password the truststore and private key. Place them in the "truststore" folder.
2. Edit the "kafka-generate-ssl.sh":
    1. Add truststore and private key passwords. Both are in 1password
    2. Specify the keystore and key passwords.
    3. Specify the CN_NAME.
3. Run the script and the keystore will be available on the "CN_NAME" folder

Do **NOT** commit this file.

#### TrustStore

The current truststore was created using the "create-truststore.sh" script. If you need to recreate it, remember that you
will need to recreate all other keystores.
 
Note that the Rest Proxy CN will have to be kafka-rest-proxy.akvotest.org
