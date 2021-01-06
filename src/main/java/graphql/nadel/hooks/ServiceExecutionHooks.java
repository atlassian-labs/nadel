package graphql.nadel.hooks;

import graphql.GraphQLError;
import graphql.PublicSpi;
import graphql.language.Field;
import graphql.nadel.engine.HooksVisitArgumentValueEnvironment;
import graphql.nadel.result.RootExecutionResultNode;
import graphql.schema.GraphQLFieldDefinition;

import java.util.Optional;
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
     * @return an async context object of your choosing
     */
    default CompletableFuture<Object> createServiceContext(CreateServiceContextParams params) {
        return CompletableFuture.completedFuture(null);
    }

    default NewVariableValue visitArgumentValueInQuery(HooksVisitArgumentValueEnvironment env) {
        return null;
    }


    default CompletableFuture<Optional<GraphQLError>> isFieldAllowed(Field field, GraphQLFieldDefinition fieldDefinitionOverall, Object userSuppliedContext) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

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
