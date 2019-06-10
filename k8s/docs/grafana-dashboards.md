Grafana is available at https://metrics.akvotest.org and https://metrics.akvo.org. 

You can login using your Github account if you belong to Akvo's Github organization.

## How to manage new dashboards

As we have two installations of Grafana, one in test, the other in production, which means that we need to keep both 
environments in sync.

We have been trying to keep both environments in sync manually, by either applying the same changes on the Grafana UI or 
by exporting/importing the dashboards as JSON.

Also, it seems that dashboards are lost when we upgrade Grafana. Seems to be something to do with Helm or maybe the way
we use Helm. In any case, we have been storing the dashboards' JSON export in each project to not lose the work.

All of this has been quite cumbersome and error prone.

Last Grafana's helm chart upgrade allows for dashboards to be defined as Kubernetes ConfigMaps, which fixes both issues. 

The expected workflow to create a new dashboard is:

1. Develop the new dashboard using the test environment
1. Export dashboard to JSON, convert it to a ConfigMap (like [this](https://github.com/akvo/akvo-rsr/blob/0a15fcf728a45d05831d0b74f80f1eaf1be8d8be/ci/k8s/grafana/main.yml)) and commit it your project.
1. Deploy the ConfigMap as any other Kubernetes artifact using your CI pipeline (see [here](https://github.com/akvo/akvo-rsr/blob/6ffee52de80f650dee7cda9cdea81b86390e8bd8/ci/deploy.sh#L56))

**Very Important**: Make sure that both the ConfigMap name and the filename of the dashboard is unique across all of Akvo. In the ConfigMap example above, the filename is `rsr-dashboard.json`. 