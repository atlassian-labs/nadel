package graphql.nadel.engine.transform.result.json

/**
 * One stable occurrence selected from a result payload.
 *
 * The selected [node] retains the response object's identity. [NadelResultView] can therefore
 * resolve its concrete path after structural result transforms have finished, rather than
 * freezing a path while the result is still in its underlying-service shape.
 */
internal data class NadelResultOccurrence(
    val node: JsonNode,
)
