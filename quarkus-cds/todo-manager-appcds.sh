#!/bin/bash

export PATH="/opt/todo-manager/jdk/bin:${PATH}"

cd /opt/todo-manager && exec java -Xshare:on -XX:SharedArchiveFile=jdk/cds/app-cds.jsa -jar todo-manager-1.0.0-SNAPSHOT-runner.jar
