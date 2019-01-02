package graphql.nadel.engine;

//public class Delegater {
//
//    private DelegatedExecution delegatedExecution;
//
//    DelegatedResultToResultNode delegatedResultToResultNode = new DelegatedResultToResultNode();
//
//    CompletableFuture<ExecutionResultNode> delegate(MergedSelectionSet query) {
//        DelegatedExecutionParameters delegatedExecutionParameters = newDelegatedExecutionParameters().build();
//        CompletableFuture<DelegatedExecutionResult> delegateResultCF = delegatedExecution.delegate(delegatedExecutionParameters);
//
//        delegateResultCF.thenApply(delegatedExecutionResult -> {
//            return delegatedResultToResultNode.resultToResultNode(delegatedExecutionResult);
//        });
//    }
//
//}
