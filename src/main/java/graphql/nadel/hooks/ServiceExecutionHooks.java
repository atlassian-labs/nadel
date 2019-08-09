package graphql.nadel.hooks;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.nadel.Service;
import graphql.schema.GraphQLSchema;

/**
 * These hooks allow you to change the way service execution happens
 */
public interface ServiceExecutionHooks {


    /**
     * Called per top level field that for a service.  This allows you to create a "context" object that will be passed into further calls.
     *
     * @param service          the service in play
     * @param topLevelStepInfo the top level field execution step info
     *
     * @return a context object of your choosing
     */
    default Object createServiceContext(Service service, ExecutionStepInfo topLevelStepInfo) {
        return null;
    }

    /**
     * Called to possibly change the arguments and runtime variables that are passed onto a called service
     *
     * @param service          the service in play
     * @param serviceContext   the context object created in {@link #createServiceContext(graphql.nadel.Service, graphql.execution.ExecutionStepInfo)}
     * @param topLevelStepInfo the top level field execution step info
     *
     * @return null to indicate NO change needed or a new set of ModifiedArguments
     */
    default ModifiedArguments modifyArguments(Service service, Object serviceContext, ExecutionStepInfo topLevelStepInfo) {
        return null;
    }

    /**
     * Called to allow a service to post process the service result in some fashion.
     *
     * @param service        the service in play
     * @param serviceContext the context object created in {@link #createServiceContext(graphql.nadel.Service, graphql.execution.ExecutionStepInfo)}
     * @param overallSchema  the overall schema
     * @param resultNode     the result
     *
     * @return a possible result node
     */
    default RootExecutionResultNode postServiceResult(Service service, Object serviceContext, GraphQLSchema overallSchema, RootExecutionResultNode resultNode) {
        return resultNode;
    }

}
