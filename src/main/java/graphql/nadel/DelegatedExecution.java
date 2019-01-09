package graphql.nadel;


import graphql.PublicApi;

import java.util.concurrent.CompletableFuture;

@PublicApi
public interface DelegatedExecution {
    CompletableFuture<DelegatedExecutionResult> delegate(DelegatedExecutionParameters delegatedExecutionParameters);
}
