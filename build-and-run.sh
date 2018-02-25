#!/usr/bin/env bash
imageID=$(docker build . -q)
docker run -p 8080:8080 -v $(pwd)/nadel-service/nadel-dsl.txt:/nadel-dsl.txt ${imageID}

