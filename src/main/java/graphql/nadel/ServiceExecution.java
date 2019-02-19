package graphql.nadel;


import graphql.PublicApi;

import java.util.concurrent.CompletableFuture;

@PublicApi
public interface ServiceExecution {
    CompletableFuture<ServiceExecutionResult> execute(ServiceExecutionParameters serviceExecutionParameters);
}
