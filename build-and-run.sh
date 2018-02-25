#!/usr/bin/env bash
set -e
./gradlew build
cd nadel-service
java -jar build/libs/nadel-service-0.0.1-SNAPSHOT.jar

