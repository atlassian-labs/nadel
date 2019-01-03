package graphql.nadel.engine;

import graphql.Internal;
import graphql.execution.ExecutionContext;
import graphql.execution.nextgen.ExecutionStrategy;
import graphql.execution.nextgen.FieldSubSelection;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;
import graphql.nadel.DelegatedExecution;
import graphql.nadel.DelegatedExecutionParameters;
import graphql.nadel.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static graphql.Assert.assertNotEmpty;
import static graphql.nadel.DelegatedExecutionParameters.newDelegatedExecutionParameters;

@Internal
public class NadelExecutionStrategy implements ExecutionStrategy {

    DelegatedResultToResultNode resultToResultNode = new DelegatedResultToResultNode();

    private final List<Service> services;

    public NadelExecutionStrategy(List<Service> services) {
        assertNotEmpty(services);
        this.services = services;
    }

    @Override
    public CompletableFuture<ObjectExecutionResultNode.RootExecutionResultNode> execute(ExecutionContext context, FieldSubSelection fieldSubSelection) {
        // we assume here that we have only have one service wth no mapping/hydration or anything else
        DelegatedExecution delegatedExecution = services.get(0).getDelegatedExecution();
        return delegate(context, fieldSubSelection, delegatedExecution);
    }


    private CompletableFuture<ObjectExecutionResultNode.RootExecutionResultNode> delegate(ExecutionContext context,
                                                                                          FieldSubSelection fieldSubSelection,
                                                                                          DelegatedExecution delegatedExecution) {
        //in the future we need to do more work here: creating a document from MergedField/MergedSelectionSet
        DelegatedExecutionParameters delegatedExecutionParameters = newDelegatedExecutionParameters()
                .query(context.getDocument())
                .build();
        return delegatedExecution.delegate(delegatedExecutionParameters)
                .thenApply(delegatedExecutionResult -> resultToResultNode.resultToResultNode(context, delegatedExecutionResult, fieldSubSelection));
    }


}
