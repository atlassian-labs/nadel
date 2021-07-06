package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.KeepHook
import graphql.nadel.tests.NadelEngineType
import graphql.nadel.tests.util.serviceExecutionFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@KeepHook
class `query-to-one-service-with-execution-input-passed-down` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        val serviceExecutionFactory = builder.serviceExecutionFactory

        return builder
            .serviceExecutionFactory(object : ServiceExecutionFactory by serviceExecutionFactory {
                override fun getServiceExecution(serviceName: String): ServiceExecution {
                    val default = serviceExecutionFactory.getServiceExecution(serviceName)

                    return ServiceExecution { params ->
                        expectThat(params)
                            .get { context }
                            .isEqualTo("contextObj")

                        default.execute(params)
                    }
                }
            })
    }

    override fun makeExecutionInput(
        engineType: NadelEngineType,
        builder: NadelExecutionInput.Builder,
    ): NadelExecutionInput.Builder {
        return builder
            .context("contextObj")
    }
}
