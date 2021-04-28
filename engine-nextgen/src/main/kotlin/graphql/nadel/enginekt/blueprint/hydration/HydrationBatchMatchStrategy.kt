package graphql.nadel.enginekt.blueprint.hydration

sealed class HydrationBatchMatchStrategy {
    object MatchIndex : HydrationBatchMatchStrategy()
    data class MatchObjectIdentifier(val objectId: String) : HydrationBatchMatchStrategy()
}
