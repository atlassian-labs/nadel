package graphql.nadel.tests.hooks

import graphql.ExecutionInput
import graphql.execution.ResultNodesInfo
import graphql.nadel.Nadel
import graphql.nadel.engine.blueprint.NadelDefaultIntrospectionRunner
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

@UseHook
class `can-specify-max-introspection-response-size` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return super.makeNadel(builder)
            .introspectionRunnerFactory { schema ->
                object : NadelDefaultIntrospectionRunner(schema) {
                    override fun makeExecutionInput(input: ExecutionInput.Builder) {
                        input.graphQLContext { context ->
                            context.put(ResultNodesInfo.MAX_RESULT_NODES, 10)
                        }
                    }
                }
            }
    }
}
