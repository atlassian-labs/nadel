package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.Nadel
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.normalized.ExecutableNormalizedField

/**
 * A custom instruction-selection hook is observable per consumer, so it forces isolation even
 * when its implementation simply selects the only available instruction.
 */
class BatchHydrationCoalescingCustomInstructionHookFallbackTest :
    BatchHydrationCoalescingEqualSelectionsTest() {
    override fun makeNadel(): Nadel.Builder {
        return super.makeNadel()
            .executionHooks(
                object : NadelExecutionHooks {
                    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
                        virtualField: ExecutableNormalizedField,
                        instructions: List<T>,
                        parentNode: JsonNode,
                        aliasHelper: NadelAliasHelper,
                        userContext: Any?,
                    ): T? {
                        return instructions.single()
                    }
                },
            )
    }
}
