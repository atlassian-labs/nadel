package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.Nadel
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationOnExceptionParameters
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.assertJsonKeys
import graphql.nadel.tests.util.data
import graphql.nadel.tests.util.errors
import graphql.nadel.tests.util.message
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.get
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.single

@UseHook
class `exceptions-in-service-execution-call-result-in-graphql-errors-and-call-onerror-instrumentation` :
    EngineTestHook {
    private class PopGoesTheWeaselException() : Exception()

    var serviceName: String? = null
    var errorMessage: String? = null
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionFactory {
                ServiceExecution {
                    throw PopGoesTheWeaselException()
                }
            }
            .instrumentation(object : NadelInstrumentation {
                override fun onException(parameters: NadelInstrumentationOnExceptionParameters) {
                    serviceName = parameters.serviceName
                }
            })
    }

    override fun assertResult(result: ExecutionResult) {
        expectThat(result).data
            .isNotNull()
            .assertJsonKeys()["hello"]
            .isNull()
        expectThat(result).errors
            .single()
            .message
            .contains("PopGoesTheWeaselException")

        expectThat(serviceName).isEqualTo("MyService")
    }
}
