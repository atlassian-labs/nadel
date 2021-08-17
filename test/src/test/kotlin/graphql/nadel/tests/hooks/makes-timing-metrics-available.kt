package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.Nadel
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.instrumentation.NadelEngineInstrumentation
import graphql.nadel.engine.result.RootExecutionResultNode
import graphql.nadel.instrumentation.parameters.NadelInstrumentRootExecutionResultParameters
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.KeepHook
import graphql.nadel.tests.NadelEngineType
import graphql.nadel.tests.util.serviceExecutionFactory
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isNotNull
import strikt.assertions.single
import java.time.Duration
import java.util.concurrent.CompletableFuture

@KeepHook
class `makes-timing-metrics-available` : EngineTestHook {
    var rootResultNode: RootExecutionResultNode? = null

    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        val serviceExecutionFactory = builder.serviceExecutionFactory

        return builder
            .serviceExecutionFactory(object : ServiceExecutionFactory by serviceExecutionFactory {
                override fun getServiceExecution(serviceName: String): ServiceExecution {
                    val default = serviceExecutionFactory.getServiceExecution(serviceName)
                    return when (serviceName) {
                        "service2" -> ServiceExecution { params ->
                            val cf = CompletableFuture<ServiceExecutionResult>()
                            Thread {
                                Thread.sleep(251)
                                cf.complete(default.execute(params).get())
                            }.start()
                            cf
                        }
                        else -> default
                    }
                }
            })
            .instrumentation(object : NadelEngineInstrumentation {
                override fun instrumentRootExecutionResult(
                    rootExecutionResultNode: RootExecutionResultNode,
                    parameters: NadelInstrumentRootExecutionResultParameters,
                ): RootExecutionResultNode {
                    return rootExecutionResultNode.also { rootResultNode = it }
                }
            })
    }

    override fun assertResult(engineType: NadelEngineType, result: ExecutionResult) {
        expectThat(rootResultNode)
            .isNotNull()
            .get { children }
            .single()
            .and {
                get { elapsedTime.duration }
                    .isLessThan(Duration.ofMillis(250L))
            }
            .and {
                get { children }
                    .single()
                    .get { elapsedTime.duration }
                    .isGreaterThan(Duration.ofMillis(250L))
            }
    }
}
