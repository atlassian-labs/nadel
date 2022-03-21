package graphql.nadel.engine.transform.result.json

data class JsonNode(
    val resultPath: JsonNodePath,
    val value: Any?,
)
