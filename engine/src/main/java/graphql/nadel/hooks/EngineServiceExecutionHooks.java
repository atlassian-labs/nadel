package graphql.nadel.hooks;

import graphql.nadel.result.RootExecutionResultNode;

import java.util.concurrent.CompletableFuture;

public interface EngineServiceExecutionHooks extends ServiceExecutionHooks {
    /**
     * Called to allow a service to post process the service result in some fashion.
     *
     * @param params the parameters to this call
     * @return an async possible result node
     */
    default CompletableFuture<RootExecutionResultNode> resultRewrite(ResultRewriteParams params) {
        return CompletableFuture.completedFuture(params.getResultNode());
    }
}
