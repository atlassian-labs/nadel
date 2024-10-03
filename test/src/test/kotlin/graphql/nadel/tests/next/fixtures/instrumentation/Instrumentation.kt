package graphql.nadel.tests.next.fixtures.instrumentation

import graphql.ExecutionResult
import graphql.execution.ExecutionId
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.SimpleInstrumentationContext
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.NadelExecutionInput
import graphql.nadel.ServiceExecution
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters
import graphql.nadel.tests.next.NadelIntegrationTest
import java.util.concurrent.CompletableFuture
import kotlin.test.assertTrue

class InstrumentationBeginQueryExecutionOnCompleteOnFailureTest : Instrumentation() {
    private var onCompleteParams: Pair<ExecutionResult?, Throwable?>? = null
    private var isBeginQueryExecutionInvoked = false
    private var isBeginExecutionInvoked = false

    override fun makeInstrumentation(): NadelInstrumentation {
        return object : NadelInstrumentation {
            override fun beginQueryExecution(
                parameters: NadelInstrumentationQueryExecutionParameters,
            ): InstrumentationContext<ExecutionResult> {
                isBeginQueryExecutionInvoked = true
                return object : SimpleInstrumentationContext<ExecutionResult>() {
                    override fun onCompleted(result: ExecutionResult?, throwable: Throwable?) {
                        assertTrue(onCompleteParams == null) // Invoked only once
                        onCompleteParams = result to throwable
                    }
                }
            }

            override fun beginExecute(
                parameters: NadelInstrumentationExecuteOperationParameters,
            ): CompletableFuture<InstrumentationContext<ExecutionResult>> {
                assertTrue(isBeginQueryExecutionInvoked)
                isBeginExecutionInvoked = true
                return super.beginExecute(parameters)
            }
        }
    }

    override fun makeServiceExecution(service: Service): ServiceExecution {
        return ServiceExecution {
            throw UnsupportedOperationException("no-op")
        }
    }

    override fun makeExecutionInput(): NadelExecutionInput.Builder {
        return super.makeExecutionInput()
            .executionId(ExecutionId.from("stable-id"))
    }

    override fun assert(result: ExecutionResult, incrementalResults: List<DelayedIncrementalPartialResult>?) {
        assertTrue(isBeginQueryExecutionInvoked)
        assertTrue(isBeginExecutionInvoked)

        val onCompleteParams = onCompleteParams
        assertTrue(onCompleteParams != null)
        val (onCompleteResult, onCompleteThrowable) = onCompleteParams
        assertTrue(onCompleteResult === result)
        assertTrue(onCompleteThrowable == null)
    }
}

class InstrumentationBeginQueryExecutionOnCompleteOnSuccessTest : Instrumentation() {
    private var onCompleteParams: Pair<ExecutionResult?, Throwable?>? = null
    private var isBeginQueryExecutionInvoked = false
    private var isBeginExecutionInvoked = false

    override fun makeInstrumentation(): NadelInstrumentation {
        return object : NadelInstrumentation {
            override fun beginQueryExecution(
                parameters: NadelInstrumentationQueryExecutionParameters,
            ): InstrumentationContext<ExecutionResult> {
                isBeginQueryExecutionInvoked = true
                return object : SimpleInstrumentationContext<ExecutionResult>() {
                    override fun onCompleted(result: ExecutionResult?, throwable: Throwable?) {
                        assertTrue(onCompleteParams == null) // Invoked only once
                        onCompleteParams = result to throwable
                    }
                }
            }

            override fun beginExecute(
                parameters: NadelInstrumentationExecuteOperationParameters,
            ): CompletableFuture<InstrumentationContext<ExecutionResult>> {
                assertTrue(isBeginQueryExecutionInvoked)
                isBeginExecutionInvoked = true
                return super.beginExecute(parameters)
            }
        }
    }

    override fun assert(result: ExecutionResult, incrementalResults: List<DelayedIncrementalPartialResult>?) {
        val onCompleteParams = onCompleteParams
        assertTrue(onCompleteParams != null)
        val (onCompleteResult, onCompleteThrowable) = onCompleteParams
        assertTrue(onCompleteResult === result)
        assertTrue(onCompleteThrowable == null)
    }
}

class InstrumentationBeginExecuteOnCompleteOnFailureTest : Instrumentation() {
    private var onCompleteParams: Pair<ExecutionResult?, Throwable?>? = null

    override fun makeInstrumentation(): NadelInstrumentation {
        return object : NadelInstrumentation {
            override fun beginExecute(
                parameters: NadelInstrumentationExecuteOperationParameters,
            ): CompletableFuture<InstrumentationContext<ExecutionResult>> {
                return CompletableFuture.completedFuture(
                    object : SimpleInstrumentationContext<ExecutionResult>() {
                        override fun onCompleted(result: ExecutionResult?, throwable: Throwable?) {
                            assertTrue(onCompleteParams == null) // Invoked only once
                            onCompleteParams = result to throwable
                        }
                    }
                )
            }
        }
    }

    override fun makeServiceExecution(service: Service): ServiceExecution {
        return ServiceExecution {
            throw UnsupportedOperationException("no-op")
        }
    }

    override fun makeExecutionInput(): NadelExecutionInput.Builder {
        return super.makeExecutionInput()
            .executionId(ExecutionId.from("stable-id"))
    }

    override fun assert(result: ExecutionResult, incrementalResults: List<DelayedIncrementalPartialResult>?) {
        val onCompleteParams = onCompleteParams
        assertTrue(onCompleteParams != null)
        val (onCompleteResult, onCompleteThrowable) = onCompleteParams
        assertTrue(onCompleteResult === result)
        assertTrue(onCompleteThrowable == null)
    }
}

class InstrumentationBeginExecuteOnCompleteOnSuccessTest : Instrumentation() {
    private var onCompleteParams: Pair<ExecutionResult?, Throwable?>? = null

    override fun makeInstrumentation(): NadelInstrumentation {
        return object : NadelInstrumentation {
            override fun beginExecute(
                parameters: NadelInstrumentationExecuteOperationParameters,
            ): CompletableFuture<InstrumentationContext<ExecutionResult>> {
                return CompletableFuture.completedFuture(
                    object : SimpleInstrumentationContext<ExecutionResult>() {
                        override fun onCompleted(result: ExecutionResult?, throwable: Throwable?) {
                            assertTrue(onCompleteParams == null) // Invoked only once
                            onCompleteParams = result to throwable
                        }
                    }
                )
            }
        }
    }

    override fun assert(result: ExecutionResult, incrementalResults: List<DelayedIncrementalPartialResult>?) {
        val onCompleteParams = onCompleteParams
        assertTrue(onCompleteParams != null)
        val (onCompleteResult, onCompleteThrowable) = onCompleteParams
        assertTrue(onCompleteResult === result)
        assertTrue(onCompleteThrowable == null)
    }
}

abstract class Instrumentation : NadelIntegrationTest(
    query = """
        query {
          echo
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "tester",
            overallSchema = """
                type Query {
                  echo: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type.dataFetcher("echo") { env ->
                            "Hello World"
                        }
                    }
            },
        ),
    ),
)
