package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.execution.ExecutionId
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput
import graphql.nadel.ServiceExecution
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.ErrorData
import graphql.nadel.instrumentation.parameters.NadelInstrumentationOnErrorParameters
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
    var serviceName: String? = null
    var errorMessage: String? = null
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .instrumentation(object : NadelInstrumentation {
                override fun onError(parameters: NadelInstrumentationOnErrorParameters) {
                    serviceName = (parameters.errorData as ErrorData.ServiceExecutionErrorData).serviceName
                    errorMessage = parameters.message
                }
            })
    }

    override fun makeExecutionInput(builder: NadelExecutionInput.Builder): NadelExecutionInput.Builder {
        return super.makeExecutionInput(builder)
            .executionId(ExecutionId.from("test"))
    }

    override fun wrapServiceExecution(
        serviceName: String,
        baseTestServiceExecution: ServiceExecution,
    ): ServiceExecution {
        return ServiceExecution {
            throw RuntimeException("Pop goes the weasel")
        }
    }

    override fun assertResult(result: ExecutionResult) {
        expectThat(result).data
            .isNotNull()
            .assertJsonKeys()["hello"]
            .isNull()
        expectThat(result).errors
            .single()
            .message
            .contains("Pop goes the weasel")

        expectThat(serviceName).isEqualTo("MyService")
        expectThat(errorMessage).isEqualTo("An exception occurred invoking the service 'MyService'")
    }
}
