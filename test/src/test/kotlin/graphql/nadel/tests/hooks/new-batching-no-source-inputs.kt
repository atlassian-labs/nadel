package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.ServiceLike
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.hooks.NadelDynamicServiceResolutionResult
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField

@UseHook
class `batching-no-source-inputs` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return super.makeNadel(builder)
            .executionHooks(
                object : NadelExecutionHooks {
                    override fun resolveServiceForField(
                        services: List<ServiceLike>,
                        executableNormalizedField: ExecutableNormalizedField,
                    ): NadelDynamicServiceResolutionResult {
                        throw UnsupportedOperationException("Not yet implemented")
                    }

                    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
                        instructions: List<T>,
                        sourceId: JsonNode,
                        userContext: Any?,
                    ): T {
                        val type = (sourceId.value as String).substringBefore("/")

                        return instructions
                            .first {
                                it.backingService.name.startsWith(type, ignoreCase = true)
                            }
                    }
                },
            )
    }
}
