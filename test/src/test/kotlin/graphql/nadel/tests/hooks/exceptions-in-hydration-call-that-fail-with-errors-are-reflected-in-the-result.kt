package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.Nadel
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.engine.util.AnyMap
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.assertJsonKeys
import graphql.nadel.tests.util.data
import graphql.nadel.tests.util.errors
import graphql.nadel.tests.util.message
import graphql.nadel.tests.util.serviceExecutionFactory
import graphql.schema.idl.TypeDefinitionRegistry
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.get
import strikt.assertions.isA
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.single

@UseHook
class `exceptions-in-hydration-call-that-fail-with-errors-are-reflected-in-the-result` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        val serviceExecutionFactory = builder.serviceExecutionFactory

        return builder
            .serviceExecutionFactory(object : ServiceExecutionFactory {
                override fun getServiceExecution(serviceName: String): ServiceExecution {
                    return when (serviceName) {
                        // This is the hydration service, we die on hydration
                        "Bar" -> ServiceExecution {
                            throw RuntimeException("Pop goes the weasel")
                        }
                        else -> serviceExecutionFactory.getServiceExecution(serviceName)
                    }
                }
            })
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
            .contains("Pop goes the weasel")
    }
}

fun Assertion.Builder<out Any>.isAJsonMap(): Assertion.Builder<JsonMap> {
    return isA<AnyMap>().assertJsonKeys()
}
