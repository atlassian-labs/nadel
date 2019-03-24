package graphql.nadel.testutils;

import graphql.nadel.ServiceExecution;
import graphql.nadel.ServiceExecutionParameters;
import graphql.nadel.ServiceExecutionResult;

import java.util.concurrent.CompletableFuture;

public class MockServiceExecution implements ServiceExecution {
    final ServiceExecutionResult result;

    public MockServiceExecution(ServiceExecutionResult result) {
        this.result = result;
    }

    @Override
    public CompletableFuture<ServiceExecutionResult> execute(ServiceExecutionParameters serviceExecutionParameters) {
        return CompletableFuture.completedFuture(result);
    }
}
