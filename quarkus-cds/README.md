# Quarkus with AppCDS

Dynamic CDS Archives ([JEP 350](https://openjdk.java.net/jeps/350)) are a feature new in JDK 13 that allows to compile an archive of classes accessed by an application at system exit, which then can used for class-data sharing in future runs of the application.
That way, classes are pre-verified and the archive file is memory-mapped, reduced in decreased start-up time.

Creating and using CDS archives requires explicit --class-path arguments, i.e. the current Quarkus runner JAR with its classpath in the MANIFEST.MF cannot be used as is. This project is an exploration for using CDS with Quarkus by

* Adjusting the runner JAR of a REST + CRUD application so it can be used with CDS
* Measuring time-to-first-response without and with CDS, following the strategy described in the [Quarkus Performance Measurement Guide](https://quarkus.io/guides/performance-measure)

To learn more about CDS, refer to this in-depth [blog post](https://blog.codefx.org/java/application-class-data-sharing/).

## Prerequisites

* Have Java 13 installed
* Have Docker Compose installed

## Preparation

Start up Postgres:

```shell
cd compose
docker-compose up
```

Build the project:

```shell
mvn clean package
```

Create/update launcher shell scripts (optional, only needed when project dependencies have changed):

```shell
java CreateScripts.java
```

Patch runner JAR to remove its classpath entry from the manifest:

```shell
./patch-runner.sh
```

Create an app CDS archive:

```shell
./prepare-cds.sh
```

Access localhost:8080/api and stop the Quarkus app.
Observe the created/updated _app-cds.jsa_ file.

## Run Measurements

In one shell session run:

```shell
while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' localhost:8080/api)" != "200" ]]; do sleep .00001; done
```

And in another one (use _gdate_ on mac os):

```shell
date +"%T.%3N" && ./runCds.sh
```

You'll see three timestamps printed out.
The difference between first and last one is the time-to-first response.
