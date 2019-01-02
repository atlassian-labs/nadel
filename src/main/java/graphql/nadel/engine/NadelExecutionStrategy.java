package graphql.nadel.engine;

import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfoFactory;
import graphql.execution.nextgen.ExecutionStrategy;
import graphql.execution.nextgen.FieldSubSelection;
import graphql.execution.nextgen.ResultNodesCreator;
import graphql.execution.nextgen.ValueFetcher;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;

import java.util.concurrent.CompletableFuture;

public class NadelExecutionStrategy implements ExecutionStrategy {

    ExecutionStepInfoFactory executionInfoFactory;
    ValueFetcher valueFetcher;
    ResultNodesCreator resultNodesCreator = new ResultNodesCreator();


    @Override
    public CompletableFuture<ObjectExecutionResultNode.RootExecutionResultNode> execute(ExecutionContext context, FieldSubSelection fieldSubSelection) {
        return null;
    }


}
