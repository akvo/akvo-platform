# Upgrade GKE clusters

Follow these steps to upgrade clusters on GKE:

1. Point kubectl to use the context of the cluster to be upgraded
2. List valid master and node versions
```
gcloud container get-server-config --zone europe-west1-d
```
3. Upgrade master:
```
gcloud container clusters upgrade <cluster_name> --master --cluster-version=<version> --zone europe-west1-d
```
4. Create a new node pool, which will use new master version:
```
gcloud container node-pools create <name> --num-nodes <num_nodes> --machine-type <machine_type> --cluster <cluster_name> --zone europe-west1-d --scopes=compute-rw,storage-ro,service-management,service-control,logging-write,monitoring-write
```
5. Get old nodes names
```
kubectl get nodes | grep <old_node_pool_name>
```
And drain all of them. This operation makes all pods to be restarted and scheduled into new nodes.
```
kubectl drain <node_name> --delete-local-data --ignore-daemonsets --force --grace-period=60
```
6. After some sanity checks, delete old node pool:
```
gcloud container node-pools delete <old_node_pool_name> --cluster <cluster_name> --zone europe-west1-d
```