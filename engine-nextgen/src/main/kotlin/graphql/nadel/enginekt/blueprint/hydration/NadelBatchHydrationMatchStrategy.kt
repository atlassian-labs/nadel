package graphql.nadel.enginekt.blueprint.hydration

sealed class NadelBatchHydrationMatchStrategy {
    object MatchIndex : NadelBatchHydrationMatchStrategy()
    data class MatchObjectIdentifier(val objectId: String) : NadelBatchHydrationMatchStrategy()
}
