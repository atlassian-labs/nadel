package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.execution.ExecutionId
import graphql.nadel.NadelExecutionInput
import graphql.nadel.ServiceExecution
import graphql.nadel.engine.util.AnyMap
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.assertJsonKeys
import graphql.nadel.tests.util.data
import graphql.nadel.tests.util.errors
import graphql.nadel.tests.util.message
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.get
import strikt.assertions.isA
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.single

@UseHook
class `exceptions-in-hydration-call-that-fail-with-errors-are-reflected-in-the-result` : EngineTestHook {
    private class PopGoesTheWeaselException : Exception()

    override fun wrapServiceExecution(serviceName: String, baseTestServiceExecution: ServiceExecution): ServiceExecution {
        return when (serviceName) {
            // This is the hydration service, we die on hydration
            "Bar" -> ServiceExecution {
                throw PopGoesTheWeaselException()
            }
            else -> baseTestServiceExecution
        }
    }

    override fun makeExecutionInput(builder: NadelExecutionInput.Builder): NadelExecutionInput.Builder {
        return super.makeExecutionInput(builder)
            .executionId(ExecutionId.from("test"))
    }

    override fun assertResult(result: ExecutionResult) {
        expectThat(result).data
            .isNotNull()
            .isAJsonMap()["foo"]
            .isNotNull()
            .isAJsonMap()["bar"]
            .isNull()
        expectThat(result).errors
            .single()
            .message
            .isEqualTo("An PopGoesTheWeaselException occurred invoking the service Bar")
    }
}

fun Assertion.Builder<out Any>.isAJsonMap(): Assertion.Builder<JsonMap> {
    return isA<AnyMap>().assertJsonKeys()
}
