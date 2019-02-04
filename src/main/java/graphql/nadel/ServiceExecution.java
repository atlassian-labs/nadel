package graphql.nadel;


import graphql.PublicApi;

import java.util.concurrent.CompletableFuture;

@PublicApi
public interface ServiceExecution {
    CompletableFuture<DelegatedExecutionResult> execute(DelegatedExecutionParameters delegatedExecutionParameters);
}
