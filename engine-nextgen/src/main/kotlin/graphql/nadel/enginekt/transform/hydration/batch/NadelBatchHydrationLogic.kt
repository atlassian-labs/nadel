package graphql.nadel.enginekt.transform.hydration.batch

import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.result.json.JsonNode

internal interface NadelBatchHydrationLogic {
    fun mapFieldPathToResultKeys(
        state: NadelBatchHydrationTransform.State,
        path: List<String>,
    ): List<String>

    fun getMatchingInstruction(
        parentNode: JsonNode,
        state: NadelBatchHydrationTransform.State,
        executionPlan: NadelExecutionPlan,
    ): NadelBatchHydrationFieldInstruction?
}
