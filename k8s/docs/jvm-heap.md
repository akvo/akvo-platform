
# Steps to take a heap dump of a JVM process in a k8s Pod

**Update**: We have followed a different approach and start the JVM
process with `-XX:+HeapDumpOnOutOfMemoryError` and dump the file in a
shared volume.  A _sidecar_ container watches that folder and uploads
any new file to a storage bucket.  More details:
https://github.com/akvo/akvo-flow-api/pull/135

Notes:

* The default tool (`jmap`) is not available by default in the JRE packages on Debian nor Alpine:
  - [openjdk-8-jdk](https://packages.debian.org/stretch/openjdk-8-jdk) on Debian
  - [openjdk8](https://pkgs.alpinelinux.org/package/v3.7/community/x86_64/openjdk8-dbg) on Alpine


## Connecting to the pod

    kubectl exec <pod> -c <container> -i -t -- <shell> -il

	kubectl exec flow-api-66f6c75bf7-tj56h -c flow-api-backend -i -t -- sh -il

## Installing package

    flow-api-66f6c75bf7-tj56h:/app# apk add --no-cache openjdk8
    fetch http://dl-cdn.alpinelinux.org/alpine/v3.7/main/x86_64/APKINDEX.tar.gz
    fetch http://dl-cdn.alpinelinux.org/alpine/v3.7/community/x86_64/APKINDEX.tar.gz
    (1/1) Installing openjdk8 (8.171.11-r0)
    Executing java-common-0.1-r0.trigger
    OK: 271 MiB in 52 packages

## Taking a heap dump

`java` process is running as PID 1

    flow-api-66f6c75bf7-tj56h:/app# ps
    PID   USER     TIME   COMMAND
    1     root     9:19   java -cp ./* org.akvo.flow_api.main

There is a known issue/limitation of trying to get a heap dump on
[openjdk/alpine with java is PID 1](https://github.com/docker-library/openjdk/issues/76)

    flow-api-66f6c75bf7-tj56h:/app# /usr/lib/jvm/java-1.8-openjdk/bin/jmap -dump:format=b,file=/tmp/heap.bin 1
    1: Unable to get pid of LinuxThreads manager thread

The workaround is to run a _proper_ init process, e.g. `runit` or `tini`


## Testing workaround

A class that just sits and wait (for testing purposes)

````java
public class Sleep {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Sleeping...");
        Thread.sleep(1000 * 60 * 60);
    }
}
````

A `Dockerfile` that uses Alpine based JRE

````
FROM openjdk:8-jre-alpine

COPY Sleep.class Sleep.class

CMD ["java", "-cp", ".", "Sleep"]
````


### Testing without `init`

    $ docker run --rm -ti --name jvm-test akvo/jvm-test
    Sleeping...

In another console


    $ docker exec jvm-test ps
    PID   USER     TIME   COMMAND
    1     root     0:00   java -cp . Sleep
    22    root     0:00   ps

Install `openjdk8`

    $ docker exec -ti jvm-test sh

	# apk add --no-cache openjdk8
    fetch http://dl-cdn.alpinelinux.org/alpine/v3.7/main/x86_64/APKINDEX.tar.gz
    fetch http://dl-cdn.alpinelinux.org/alpine/v3.7/community/x86_64/APKINDEX.tar.gz
    (1/1) Installing openjdk8 (8.171.11-r0)
    Executing java-common-0.1-r0.trigger
    OK: 100 MiB in 51 packages

Try to take a heap dump with `jmap`


    # /usr/lib/jvm/java-1.8-openjdk/bin/jmap -dump:format=b,file=/tmp/heap.bin 1
    1: Unable to get pid of LinuxThreads manager thread


### Testing with `init`

    $ docker run --rm -ti --name jvm-test --init akvo/jvm-test
    Sleeping...

In another console

    $ docker exec jvm-test ps
    PID   USER     TIME   COMMAND
    1     root     0:00   /dev/init -- java -cp . Sleep
    8     root     0:00   java -cp . Sleep
    23    root     0:00   ps

Installing `openjdk`

    $ docker exec -it jvm-test sh

    # apk add --no-cache openjdk8
	fetch http://dl-cdn.alpinelinux.org/alpine/v3.7/main/x86_64/APKINDEX.tar.gz
    fetch http://dl-cdn.alpinelinux.org/alpine/v3.7/community/x86_64/APKINDEX.tar.gz
    (1/1) Installing openjdk8 (8.171.11-r0)
    Executing java-common-0.1-r0.trigger
    OK: 100 MiB in 51 packages

Taking heap dump of java process PID 8

    # /usr/lib/jvm/java-1.8-openjdk/bin/jmap -dump:format=b,file=/tmp/heap.bin 8
    Dumping heap to /tmp/heap.bin ...
    Heap dump file created
