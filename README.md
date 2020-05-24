# Quarkus with AppCDS

Dynamic CDS Archives ([JEP 350](https://openjdk.java.net/jeps/350)) are an improvement in JDK 13 that allows to archive the loaded application and library classes at application exit.
The archive then can used for class-data sharing in future runs of the application.
The archive file will memory-mapped, directly restoring class metadata, resulting in decreased start-up time.

This project is an exploration for using CDS with Quarkus by measuring time-to-first-response without and with CDS, following the strategy described in the [Quarkus Performance Measurement Guide](https://quarkus.io/guides/performance-measure)

To learn more about CDS, refer to this in-depth [blog post](https://blog.codefx.org/java/application-class-data-sharing/).

## Prerequisites

* Have Java 14 or newer installed
* Have Docker Compose installed

## Preparation

Start up Postgres:

```shell
cd compose
docker-compose up
```

Or this without Compose:

```shell
docker run -d -p 5432:5432 --name pgdemodb -v $(pwd)/init.sql:/docker-entrypoint-initdb.d/init.sql -e POSTGRES_USER=todouser -e POSTGRES_PASSWORD=todopw -e POSTGRES_DB=tododb postgres:11
```

Build the project:

```shell
cd quarkus-cds
mvn clean verify
```

As part of the build, an app CDS archive is created at _target/app-cds.jsa_.
For that purpose, the Quarkus application is started in the `pre-integration-test` phase,
then the API endpoint is invoked by means of a RESTAssured based test,
triggering the loading of all relevant classes.
The file should have a size of ~40 MB.

## Run Measurements

In one shell session run:

```shell
while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' localhost:8080/api)" != "200" ]]; do sleep .00001; done
```

And in another one (use _gdate_ on mac os):

```shell
date +"%T.%3N" && ./run-cds.sh
```

You'll see three timestamps printed out.
The difference between first and last one is the time-to-first response.

On my machine, time-to-first-response is 2s326ms without CDS and 1s588ms with CDS.

Check _target/classload.log_ and observe how dependency classes e.g. from Hibernate, Netty or Vert.x are loaded from "shared objects file (top)"; if you see them loaded from dependency JARs instead, something went wrong with either setting up or using the class data archive.

## Misc.

Getting a shell in the database if needed:

```shell
docker run --tty --rm -i --network cds-network debezium/tooling bash -c 'pgcli postgresql://todouser:todopw@todo-db:5432/tododb'
```
