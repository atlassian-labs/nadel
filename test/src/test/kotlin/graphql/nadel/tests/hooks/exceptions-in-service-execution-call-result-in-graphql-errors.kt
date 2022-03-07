package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.Nadel
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.assertJsonKeys
import graphql.nadel.tests.util.data
import graphql.nadel.tests.util.errors
import graphql.nadel.tests.util.message
import graphql.nadel.tests.util.serviceExecutionFactory
import graphql.schema.idl.TypeDefinitionRegistry
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.get
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.single

@UseHook
class `exceptions-in-service-execution-call-result-in-graphql-errors` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        val serviceExecutionFactory = builder.serviceExecutionFactory

        return builder
            .serviceExecutionFactory(object : ServiceExecutionFactory {
                override fun getServiceExecution(serviceName: String): ServiceExecution {
                    return ServiceExecution {
                        throw RuntimeException("Pop goes the weasel")
                    }
                }

                override fun getUnderlyingTypeDefinitions(serviceName: String): TypeDefinitionRegistry {
                    return serviceExecutionFactory.getUnderlyingTypeDefinitions(serviceName)
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
            .contains("Pop goes the weasel")
    }
}
