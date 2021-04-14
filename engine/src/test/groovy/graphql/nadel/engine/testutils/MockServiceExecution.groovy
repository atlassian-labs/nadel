package graphql.nadel.engine.testutils

import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionParameters
import graphql.nadel.ServiceExecutionResult

import java.util.concurrent.CompletableFuture

class MockServiceExecution implements ServiceExecution {
    final ServiceExecutionResult result;

    MockServiceExecution(Map<String, Object> data) {
        this(data, Collections.emptyList())
    }

    MockServiceExecution(Map<String, Object> data, List<Map<String, Object>> errors) {
        this.result = new ServiceExecutionResult(data, errors)
    }

    @Override
    CompletableFuture<ServiceExecutionResult> execute(ServiceExecutionParameters serviceExecutionParameters) {
        return CompletableFuture.completedFuture(result)
    }
}
