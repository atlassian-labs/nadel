package graphql.nadel.dsl

data class NadelHydrationDefinition(
    val serviceName: String,
    val pathToActorField: List<String>,
    val arguments: List<RemoteArgumentDefinition>,
    val objectIdentifier: String?,
    val objectIdentifiers: List<ObjectIdentifier>?,
    val isObjectMatchByIndex: Boolean,
    val isBatched: Boolean,
    val batchSize: Int,
    val timeout: Int,
    val condition: NadelHydrationResultConditionDefinition?
) {
    data class ObjectIdentifier(val sourceId: String, val resultId: String)
}
