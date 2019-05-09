package graphql.nadel;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.language.Argument;
import graphql.schema.GraphQLSchema;

import java.util.List;

public interface ServiceExecutionHooks {


    default Object createServiceContext(Service service, ExecutionStepInfo topLevelStepInfo) {
        return null;
    }

    default List<Argument> modifyArguments(Service service, Object serviceContext, ExecutionStepInfo topLevelStepInfo, List<Argument> arguments) {
        return null;
    }

    default RootExecutionResultNode postServiceResult(Service service, Object serviceContext, GraphQLSchema overallSchema, RootExecutionResultNode resultNode) {
        return resultNode;
    }

}
