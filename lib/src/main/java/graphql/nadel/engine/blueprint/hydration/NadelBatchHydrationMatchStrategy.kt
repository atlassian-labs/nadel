package graphql.nadel.engine.blueprint.hydration

import graphql.nadel.engine.transform.query.NadelQueryPath

sealed class NadelBatchHydrationMatchStrategy {
    object MatchIndex : NadelBatchHydrationMatchStrategy()

    data class MatchObjectIdentifier(
        val sourceId: NadelQueryPath,
        // todo should also be NadelQueryPath
        val resultId: String,
    ) : NadelBatchHydrationMatchStrategy()

    data class MatchObjectIdentifiers(
        val objectIds: List<MatchObjectIdentifier>,
    ) : NadelBatchHydrationMatchStrategy()
}
