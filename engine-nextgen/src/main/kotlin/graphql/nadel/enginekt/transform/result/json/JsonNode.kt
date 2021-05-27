package graphql.nadel.enginekt.transform.result.json

data class JsonNode(
    val path: JsonNodePath,
    val value: Any?,
)
