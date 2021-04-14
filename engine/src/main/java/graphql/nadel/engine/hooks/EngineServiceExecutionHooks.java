package graphql.nadel.engine.hooks;

import graphql.nadel.engine.result.RootExecutionResultNode;
import graphql.nadel.hooks.ServiceExecutionHooks;

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
