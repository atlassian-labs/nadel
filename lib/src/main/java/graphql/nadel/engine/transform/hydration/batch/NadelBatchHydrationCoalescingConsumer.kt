package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy

/**
 * A prepared hydration whose matching strategy has been proven safe for coalescing.
 *
 * Indexed matching deliberately has no representation here. It remains supported by the
 * ordinary isolated hydrator, but cannot cross this boundary into shared planning or execution.
 */
internal class NadelBatchHydrationCoalescingConsumer private constructor(
    val stableId: Int,
    val hydration: NadelNewBatchHydrator.PreparedBatchHydration,
    val instruction: NadelBatchHydrationFieldInstruction,
    val objectIdentifiers: List<NadelBatchHydrationMatchStrategy.MatchObjectIdentifier>,
) {
    val context: NadelBatchHydratorContext
        get() = hydration.context

    val executionBlueprint
        get() = context.executionBlueprint

    val sourceObjectsMetadata: List<NadelNewBatchHydrator.SourceObjectMetadata>
        get() = hydration.sourceObjectsMetadata

    val selectionSignature: NadelBatchHydrationSelectionSignature by lazy(LazyThreadSafetyMode.NONE) {
        NadelBatchHydrationSelectionSignature.from(
            context.sourceField.children,
        )
    }

    companion object {
        fun createOrNull(
            stableId: Int,
            hydration: NadelNewBatchHydrator.PreparedBatchHydration,
            instruction: NadelBatchHydrationFieldInstruction,
        ): NadelBatchHydrationCoalescingConsumer? {
            val objectIdentifiers = when (val strategy = instruction.batchHydrationMatchStrategy) {
                is NadelBatchHydrationMatchStrategy.MatchIndex -> return null
                is NadelBatchHydrationMatchStrategy.MatchObjectIdentifier -> listOf(strategy)
                is NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers -> strategy.objectIds
                    .takeIf { it.isNotEmpty() }
                    ?.toList()
                    ?: return null
            }

            return NadelBatchHydrationCoalescingConsumer(
                stableId = stableId,
                hydration = hydration,
                instruction = instruction,
                objectIdentifiers = objectIdentifiers,
            )
        }
    }
}
