package graphql.nadel.enginekt.transform.query

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedQuery

class SomethingToSplitFields(
    private val blueprint: NadelOverallExecutionBlueprint,
) {
    fun getFieldsToExecute(query: NormalizedQuery): List<FieldExecution> {
        return query.topLevelFields.map {
            FieldExecution(field = it)
        }
    }
}

data class FieldExecution(
    val field: NormalizedField,
    val service: Service,
)
