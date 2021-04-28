package graphql.nadel.enginekt.transform.schema

import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.normalized.NormalizedField

class GraphQLSchemaTransformer(
    private val queryTransformer: GraphQLSchemaQueryTransformer = GraphQLSchemaQueryTransformer(),
    private val resultTransformer: GraphQLSchemaResultTransformer = GraphQLSchemaResultTransformer(),
) {
    fun transformQuery(executionPlan: NadelExecutionPlan, normalizedField: NormalizedField): NormalizedField {
        return queryTransformer.transform(executionPlan, normalizedField)
    }

    fun transformResult(executionPlan: NadelExecutionPlan, result: ServiceExecutionResult): ServiceExecutionResult {
        return resultTransformer.transform(executionPlan, result)
    }
}
