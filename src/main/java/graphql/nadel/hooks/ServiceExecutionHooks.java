package graphql.nadel.hooks;

import graphql.PublicSpi;
import graphql.execution.nextgen.result.RootExecutionResultNode;

import java.util.concurrent.CompletableFuture;

/**
 * These hooks allow you to change the way service execution happens
 */
@PublicSpi
public interface ServiceExecutionHooks {


    /**
     * Called per top level field for a service.  This allows you to create a "context" object that will be passed into further calls.
     *
     * @param params the parameters to this call
     *
     * @return an async context object of your choosing
     */
    default CompletableFuture<Object> createServiceContext(CreateServiceContextParams params) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called to possibly change the arguments and runtime variables that are passed onto a called service
     *
     * @param params the parameters to this call
     *
     * @return an async null to indicate NO change needed or an async new document and variables
     */
    default CompletableFuture<QueryRewriteResult> queryRewrite(QueryRewriteParams params) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called to allow a service to post process the service result in some fashion.
     *
     * @param params the parameters to this call
     *
     * @return an async possible result node
     */
    default CompletableFuture<RootExecutionResultNode> resultRewrite(ResultRewriteParams params) {
        return CompletableFuture.completedFuture(params.getResultNode());
    }

}
