#!/bin/bash

export PATH="/opt/todo-manager/jdk/bin:${PATH}"

cd /opt/todo-manager && exec java -jar todo-manager-1.0.0-SNAPSHOT-runner.jar
