package graphql.nadel;


import graphql.PublicSpi;

import java.util.concurrent.CompletableFuture;

@PublicSpi
public interface ServiceExecution {
    CompletableFuture<ServiceExecutionResult> execute(ServiceExecutionParameters serviceExecutionParameters);
}
