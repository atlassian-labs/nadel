package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.result.json.JsonNode

internal data class NadelResolvedObjectBatch(
    val sourceInputs: List<JsonNode>,
    val result: ServiceExecutionResult,
    /**
     * The actual result path used by this backing query.
     *
     * Shared batch hydration requests alias their backing roots so multiple independent
     * selections can coexist in one GraphQL operation. Isolated hydrations keep using the
     * instruction's original backing path.
     */
    val resultPath: NadelQueryPath? = null,
)
