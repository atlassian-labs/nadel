package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.transform.result.json.JsonNode

internal data class NadelResolvedObjectBatch(
    val sourceInputs: List<JsonNode>,
    val result: ServiceExecutionResult,
)
