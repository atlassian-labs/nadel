package graphql.nadel.hooks;

import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.nadel.Service;
import graphql.schema.GraphQLSchema;

import java.util.concurrent.CompletableFuture;

/**
 * These hooks allow you to change the way service execution happens
 */
public interface ServiceExecutionHooks {


    /**
     * Called per top level field that for a service.  This allows you to create a "context" object that will be passed into further calls.
     *
     * @param executionCtx     the {@link graphql.execution.ExecutionContext} in play
     * @param service          the service in play
     * @param topLevelStepInfo the top level field execution step info
     *
     * @return an async context object of your choosing
     */
    default CompletableFuture<Object> createServiceContext(ExecutionContext executionCtx, Service service, ExecutionStepInfo topLevelStepInfo) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called to possibly change the arguments and runtime variables that are passed onto a called service
     *
     * @param service          the service in play
     * @param serviceContext   the context object created in {@link #createServiceContext(graphql.execution.ExecutionContext, graphql.nadel.Service, graphql.execution.ExecutionStepInfo)}
     * @param topLevelStepInfo the top level field execution step info
     *
     * @return an async null to indicate NO change needed or an async new set of ModifiedArguments
     */
    default CompletableFuture<ModifiedArguments> modifyArguments(Service service, Object serviceContext, ExecutionStepInfo topLevelStepInfo) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called to allow a service to post process the service result in some fashion.
     *
     * @param service        the service in play
     * @param serviceContext the context object created in {@link #createServiceContext(graphql.execution.ExecutionContext, graphql.nadel.Service, graphql.execution.ExecutionStepInfo)}
     * @param overallSchema  the overall schema
     * @param resultNode     the result
     *
     * @return an async possible result node
     */
    default CompletableFuture<RootExecutionResultNode> postServiceResult(Service service, Object serviceContext, GraphQLSchema overallSchema, RootExecutionResultNode resultNode) {
        return CompletableFuture.completedFuture(resultNode);
    }

}
