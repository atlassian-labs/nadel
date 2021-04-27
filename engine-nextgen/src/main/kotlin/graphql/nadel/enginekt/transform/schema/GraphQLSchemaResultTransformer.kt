package graphql.nadel.enginekt.transform.schema

import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.plan.GraphQLExecutionPlan

class GraphQLSchemaResultTransformer {
    fun transform(plan: GraphQLExecutionPlan,result: ServiceExecutionResult): ServiceExecutionResult {
        return result
    }
}
