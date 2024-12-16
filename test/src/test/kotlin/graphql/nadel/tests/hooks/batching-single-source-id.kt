package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.ServiceLike
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgument.ValueSource
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.util.singleOfType
import graphql.nadel.hooks.NadelDynamicServiceResolutionResult
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField

@UseHook
class `batching-single-source-id` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return super.makeNadel(builder)
            .executionHooks(
                object : NadelExecutionHooks {
                    override fun resolveServiceForField(
                        services: List<ServiceLike>,
                        executableNormalizedField: ExecutableNormalizedField,
                    ): NadelDynamicServiceResolutionResult {
                        throw UnsupportedOperationException()
                    }

                    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
                        instructions: List<T>,
                        parentNode: JsonNode,
                        aliasHelper: NadelAliasHelper,
                        userContext: Any?,
                    ): T? {
                        return instructions
                            .single { instruction ->
                                val fieldSource = instruction.backingFieldArguments
                                    .asSequence()
                                    .map {
                                        it.valueSource
                                    }
                                    .singleOfType<ValueSource.FieldResultValue>()

                                val sourceNodes = JsonNodeExtractor.getNodesAt(
                                    parentNode,
                                    aliasHelper.getQueryPath(fieldSource.queryPathToField)
                                )
                                val sourceIdType = (sourceNodes.single().value as String).substringBefore("/")

                                instruction.backingService.name.startsWith(sourceIdType)
                            }
                    }
                },
            )
    }
}
