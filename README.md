# Quarkus with AppCDS

Dynamic CDS Archives ([JEP 350](https://openjdk.java.net/jeps/350)) are an improvement in JDK 13 that allows to archive the loaded application and library classes at application exit.
The archive then can used for class-data sharing in future runs of the application.
The archive file will memory-mapped, directly restoring class metadata, resulting in decreased start-up time.

This project is an exploration for using CDS with Quarkus by measuring time-to-first-response without and with CDS, following the strategy described in the [Quarkus Performance Measurement Guide](https://quarkus.io/guides/performance-measure)

To learn more about CDS, refer to this in-depth [blog post](https://blog.codefx.org/java/application-class-data-sharing/).

## Prerequisites

* Have Java 15 or newer installed
* Have Docker Compose installed

## Preparation

Start up Postgres:

```shell
cd compose
docker-compose up
```

Or this without Compose:

```shell
docker run -d -p 5432:5432 --name pgdemodb -v $(pwd)/compose/init.sql:/docker-entrypoint-initdb.d/init.sql -e POSTGRES_USER=todouser -e POSTGRES_PASSWORD=todopw -e POSTGRES_DB=tododb postgres:13
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

## Creating a Modular Runtime Image with AppCDS Archive

Obtain list of modules to include in the image:

```shell
jdeps --print-module-deps --multi-release 15 --ignore-missing-deps \
  --module-path target/lib/jakarta.activation.jakarta.activation-api-1.2.1.jar:target/lib/org.reactivestreams.reactive-streams-1.0.3.jar:target/lib/org.jboss.spec.javax.xml.bind.jboss-jaxb-api_2.3_spec-2.0.0.Final.jar \
  --class-path target/lib/* \
target/todo-manager-1.0.0-SNAPSHOT-runner.jar
```

```shell
$JAVA_HOME/bin/jlink --add-modules java.base,java.compiler,java.instrument,java.naming,java.rmi,java.security.jgss,java.security.sasl,java.sql,jdk.jconsole,jdk.unsupported \
  --output target/runtime-image
```

```shell
./target/runtime-image/bin/java -Xshare:dump -version
```

```shell
mkdir target/runtime-image/cds

./target/runtime-image/bin/java \
  -XX:ArchiveClassesAtExit=target/runtime-image/cds/app-cds.jsa \
  -jar target/todo-manager-1.0.0-SNAPSHOT-runner.jar
```

```shell
./target/runtime-image/bin/java -Xshare:on \
  -XX:SharedArchiveFile=target/runtime-image/cds/app-cds.jsa \
  -jar target/todo-manager-1.0.0-SNAPSHOT-runner.jar
```

## Reproducer for Seg Fault with JDK 16

Build:

```shell
cd quarkus-cds
mvn clean verify -DskipTests=true -DskipITs=true
```

```shell
java --version
openjdk 16-ea 2021-03-16
OpenJDK Runtime Environment (build 16-ea+27-1884)
OpenJDK 64-Bit Server VM (build 16-ea+27-1884, mixed mode, sharing)
```

Create the runtime image:

```shell
$JAVA_HOME/bin/jlink --add-modules java.base,java.compiler,java.instrument,java.naming,java.rmi,java.security.jgss,java.security.sasl,java.sql,jdk.jconsole,jdk.unsupported \
  --output target/runtime-image
```

```shell
./target/runtime-image/bin/java -Xshare:dump -version
```

Create the class list file:

```shell
./target/runtime-image/bin/java -XX:DumpLoadedClassList=target/todo-manager-classlist.lst \
  -jar target/todo-manager-1.0.0-SNAPSHOT-runner.jar

Crtl + C
```

Observe the seg fault :)

```shell
./target/runtime-image/bin/java -Xshare:dump \
  -XX:SharedClassListFile=target/todo-manager-classlist.lst \
  -XX:SharedArchiveFile=target/runtime-image/cds/app-cds.jsa \
  -jar target/todo-manager-1.0.0-SNAPSHOT-runner.jar
```

```shell
#
# A fatal error has been detected by the Java Runtime Environment:
#
#  SIGSEGV (0xb) at pc=0x0000000109cdcc9c, pid=6375, tid=7427
#
# JRE version: OpenJDK Runtime Environment (16.0+27) (build 16-ea+27-1884)
# Java VM: OpenJDK 64-Bit Server VM (16-ea+27-1884, interpreted mode, tiered, compressed oops, g1 gc, bsd-amd64)
# Problematic frame:
# V  [libjvm.dylib+0x2dcc9c]  ClassListParser::resolve_indy(Symbol*, Thread*)+0xcc
#
```