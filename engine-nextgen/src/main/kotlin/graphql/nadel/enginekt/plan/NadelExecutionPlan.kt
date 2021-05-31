package graphql.nadel.enginekt.plan

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelTypeRenameInstruction
import graphql.nadel.enginekt.transform.query.NadelTransform
import graphql.normalized.NormalizedField

internal typealias AnyNadelExecutionPlanStep = NadelExecutionPlan.Step<Any>

/**
 * Currently per service. TODO: we should have an overall execution plan.
 */
data class NadelExecutionPlan(
    // this is a map for overall Fields
    val transformationSteps: Map<NormalizedField, List<AnyNadelExecutionPlanStep>>,
    // these are the relevant type names for the service and current query from
    // the key is the overall type name
    val typeRenames: Map<String, NadelTypeRenameInstruction>,
) {
    data class Step<T : Any>(
        val service: Service,
        val field: NormalizedField,
        val transform: NadelTransform<T>,
        val state: T,
    )

    fun getOverallTypeName(underlyingTypeName: String): String {
        val typeRenameInstruction = typeRenames
            .asSequence()
            .map { it.value }
            .filter { it.underlyingName == underlyingTypeName }
            .singleOrNull()
        return typeRenameInstruction?.overallName ?: underlyingTypeName
    }

    fun getUnderlyingTypeName(overallTypeName: String): String {
        return typeRenames[overallTypeName]?.underlyingName ?: overallTypeName
    }
}

