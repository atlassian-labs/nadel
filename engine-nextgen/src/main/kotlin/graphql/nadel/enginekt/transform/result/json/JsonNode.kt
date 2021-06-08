package graphql.nadel.enginekt.transform.result.json

data class JsonNode(
    val resultPath: JsonNodePath,
    val value: Any?,
)
