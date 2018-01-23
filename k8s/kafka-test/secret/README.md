The rest proxy needs a truststore and keystore. 

The truststore is at 1password and must be named kafka.truststore.jks
The keystore is at 1password and must be named kafka.keystore.jks. It must have the CN kafka-rest-proxy.akvotest.org

The passwords must be added to the kafka-rest.properties file. Then run:

````
kubectl create --namespace kafka secret generic confluent-secret \
        --from-file=./kafka.keystore.jks \
        --from-file=./kafka.truststore.jks \
        --from-file=./kafka-rest.properties \
        --from-file=./log4j.properties
````       

The log4j doesnt really need to be a secret but this way we avoid having a ConfigMap just for it.