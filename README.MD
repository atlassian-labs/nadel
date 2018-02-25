# Nadel: A GraphQL Stitching DSL 

This repo consists of two projects:

[Nadel-DSL](nadel-dsl): Java Library to combine several GraphQL endpoints declaratively.

[Nadel-Service](nadel-service): Service using the Nadel-DSL.


## Quickstart to run the service:

1. Java 9 is required

2. Build and run the service with an [example DSL](nadel-service/nadel-dsl.txt):

Execute in the root dir:
```bash
    ./build-and-run.sh
```

3. GraphQL is available at http://localhost:8080/graphql
