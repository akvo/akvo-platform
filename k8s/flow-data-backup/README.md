This is a Kubernetes cron job that backups the data from all Flow instances.

The backup is initiated using the service account found in the akvo-flow-server-config repository. 

The main code is at src/backup.sh, which it does its best to create the required buckets and enable the required APIs.
 
See the src/fix.instance.sh script for some one-off configuration fixes.

src/restore.sh contains the instructions to restore a backup.

Back ups are run weekly (Sunday midnight). See [here](ci/k8s/cronjob.yaml.template#L6).

EventQueue and system kinds are not backed up. See [here](src/kinds.py#L17) for details.