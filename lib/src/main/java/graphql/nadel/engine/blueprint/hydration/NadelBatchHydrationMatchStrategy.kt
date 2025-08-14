package graphql.nadel.engine.blueprint.hydration

import graphql.nadel.engine.blueprint.hydration.NadelObjectIdentifierCastingStrategy.TO_STRING
import graphql.nadel.engine.transform.query.NadelQueryPath

sealed class NadelBatchHydrationMatchStrategy {
    data object MatchIndex : NadelBatchHydrationMatchStrategy()

    /**
     * todo: this by itself should not be a strategy, use [MatchObjectIdentifiers] instead.
     */
    data class MatchObjectIdentifier(
        val sourceIdCast: NadelObjectIdentifierCastingStrategy,
        val sourceId: NadelQueryPath,
        // todo should also be NadelQueryPath
        val resultId: String,
    ) : NadelBatchHydrationMatchStrategy()

    data class MatchObjectIdentifiers(
        val objectIds: List<MatchObjectIdentifier>,
    ) : NadelBatchHydrationMatchStrategy()
}

/**
 * Tells us if we need to cast the value.
 *
 * e.g. if the source identifier is a [Long] but the result identifier is a [String]
 * then we should set [NadelBatchHydrationMatchStrategy.MatchObjectIdentifier.sourceIdCast]
 * to [TO_STRING]
 */
enum class NadelObjectIdentifierCastingStrategy {
    TO_STRING,
    NO_CAST,
    ;
}
