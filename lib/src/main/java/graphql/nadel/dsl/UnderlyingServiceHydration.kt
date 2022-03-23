package graphql.nadel.dsl

data class UnderlyingServiceHydration(
    val serviceName: String,
    val pathToActorField: List<String>,
    val arguments: List<RemoteArgumentDefinition>,
    val objectIdentifier: String?,
    val objectIdentifiers: List<ObjectIdentifier>?,
    val isObjectMatchByIndex: Boolean,
    val isBatched: Boolean,
    val batchSize: Int,
    val timeout: Int,
) {
    data class ObjectIdentifier(val sourceId: String, val resultId: String)
}
