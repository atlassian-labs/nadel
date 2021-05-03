package graphql.nadel.enginekt.transform.schema

import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.normalized.NormalizedField

class NadelSchemaQueryTransformer {
    fun transform(plan: NadelExecutionPlan, field: NormalizedField): NormalizedField {
        return field
    }
}
