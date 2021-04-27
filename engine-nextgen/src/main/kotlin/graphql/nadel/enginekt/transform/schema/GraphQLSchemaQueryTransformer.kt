package graphql.nadel.enginekt.transform.schema

import graphql.nadel.enginekt.plan.GraphQLExecutionPlan
import graphql.normalized.NormalizedField

class GraphQLSchemaQueryTransformer {
    fun transform(plan: GraphQLExecutionPlan, field: NormalizedField): NormalizedField {
        return field
    }
}
