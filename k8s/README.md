# Rollback a deployment

This is a shorter version of [the official doc](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#rolling-back-a-deployment)

#### To rollback to the previous version:
1. Make sure that one and only one person is going to perform the rollback.
1. Make sure `kubectl` is pointing to the correct environment.  
    * The best way to change environments is to use [kubctx](https://github.com/ahmetb/kubectx/).
2. Run `kubectl rollout undo deployment $$the_deployment$$`
    * You have the deployment name in the deployment yaml file in your source code, or look for it using `kubectl get deployment`.

#### Rollback to any previous version, checking that is the expected version:
1. Make sure that one and only one person is going to perform the rollback.
1. Make sure `kubectl` is pointing to the correct environment:
    ```
    $ kubectx
    gke_akvo-lumen_europe-west1-d_production
    *gke_akvo-lumen_europe-west1-d_test*
    ```
1. Check all deploy revisions with `kubectl rollout history deployment $$the_deployment$$`:
    ```
    $ kubectl rollout history deployment rsr
    deployment.extensions/rsr
    REVISION  CHANGE-CAUSE
    413       <none>
    414       <none>
    415       <none>
    416       <none>
    417       <none>
    ```
1. Find the git commit of a revision with `kubectl rollout history deployment $$the_deployment$$ --revision=$$revision_id$$ | grep version`
    ```
    $ kubectl rollout history deployment rsr --revision=415 | grep version
        rsr-version=ce4a7c2bb0e4c44cf528d207c685f3c9544fd6cf
        Liveness:	exec [sh -c echo stats | nc 127.0.0.1 11211 | grep version] delay=10s timeout=1s period=5s #success=1 #failure=3
    ```
1. Check that the git commit corresponds to the desired version.
1. Revert to the desired version with `kubectl rollout undo deployment $$the_deployment$$ --to-revision=$$revision_id$$`
    ```
    $ kubectl rollout undo deployment rsr --to-revision=415
    deployment.extensions/rsr rolled back
    ```