package graphql.nadel.engine.blueprint.hydration

import graphql.nadel.engine.transform.query.NadelQueryPath

sealed class NadelBatchHydrationMatchStrategy {
    data object MatchIndex : NadelBatchHydrationMatchStrategy()

    /**
     * todo: this by itself should not be a strategy, use [MatchObjectIdentifiers] instead.
     */
    data class MatchObjectIdentifier(
        val sourceId: NadelQueryPath,
        // todo should also be NadelQueryPath
        val resultId: String,
    ) : NadelBatchHydrationMatchStrategy()

    data class MatchObjectIdentifiers(
        val objectIds: List<MatchObjectIdentifier>,
    ) : NadelBatchHydrationMatchStrategy()
}
