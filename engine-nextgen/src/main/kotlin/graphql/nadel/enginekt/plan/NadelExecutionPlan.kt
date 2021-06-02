package graphql.nadel.enginekt.plan

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelTypeRenameInstruction
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.normalized.NormalizedField

internal typealias AnyNadelExecutionPlanStep = NadelExecutionPlan.Step<Any>

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

    /**
     * Creates and returns a new [NadelExecutionPlan] that is a merging of `this` plan
     * and the [other] plan.
     */
    fun merge(other: NadelExecutionPlan): NadelExecutionPlan {
        val newSteps = transformationSteps.toMutableMap()
        other.transformationSteps.forEach { (field, steps) ->
            newSteps.compute(field) { _, oldSteps ->
                oldSteps?.let { it + steps } ?: steps
            }
        }

        return copy(
            transformationSteps = newSteps,
            typeRenames = typeRenames + typeRenames,
        )
    }
}
