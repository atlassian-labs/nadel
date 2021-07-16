package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.execution.instrumentation.InstrumentationState
import graphql.nadel.Nadel
import graphql.nadel.engine.instrumentation.NadelEngineInstrumentation
import graphql.nadel.engine.result.ResultNodesUtil
import graphql.nadel.engine.result.RootExecutionResultNode
import graphql.nadel.instrumentation.parameters.NadelInstrumentRootExecutionResultParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationCreateStateParameters
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.KeepHook
import graphql.nadel.tests.NadelEngineType
import graphql.nadel.tests.util.getHashCode
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import graphql.GraphqlErrorException as GraphQLErrorException

@KeepHook
class `can-instrument-root-execution-result` : EngineTestHook {
    var originalExecutionResult: RootExecutionResultNode? = null
    var instrumentationParams: NadelInstrumentRootExecutionResultParameters? = null

    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        if (engineType != NadelEngineType.current) {
            error("Test only supported by current engine")
        }

        return builder
            .instrumentation(object : NadelEngineInstrumentation {
                override fun createState(parameters: NadelInstrumentationCreateStateParameters): InstrumentationState {
                    return object : InstrumentationState {
                        override fun hashCode(): Int {
                            return "test-instrumentation-state".hashCode()
                        }
                    }
                }

                override fun instrumentRootExecutionResult(
                    rootExecutionResultNode: RootExecutionResultNode,
                    parameters: NadelInstrumentRootExecutionResultParameters,
                ): RootExecutionResultNode {
                    originalExecutionResult = rootExecutionResultNode
                    instrumentationParams = parameters

                    return rootExecutionResultNode.withNewErrors(
                        listOf(
                            GraphQLErrorException.newErrorException()
                                .message("instrumented-error")
                                .build()
                        ),
                    ) as RootExecutionResultNode
                }
            })
    }

    override fun assertResult(engineType: NadelEngineType, result: ExecutionResult) {
        expectThat(instrumentationParams)
            .isNotNull()
            .get {
                getInstrumentationState<InstrumentationState>()
            }
            .getHashCode()
            .isEqualTo("test-instrumentation-state".hashCode())
        expectThat(instrumentationParams)
            .isNotNull()
            .get { executionContext }
            .get { operationDefinition }
            .get { name }
            .isEqualTo("OpName")
        expectThat(originalExecutionResult)
            .isNotNull()
            .get(ResultNodesUtil::toExecutionResult)
            .get { errors }
            .isEmpty()
    }
}
