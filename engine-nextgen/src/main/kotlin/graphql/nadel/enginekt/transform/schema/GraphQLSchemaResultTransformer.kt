package graphql.nadel.enginekt.transform.schema

import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.plan.NadelExecutionPlan

class GraphQLSchemaResultTransformer {
    fun transform(plan: NadelExecutionPlan, result: ServiceExecutionResult): ServiceExecutionResult {
        return result
    }
}
