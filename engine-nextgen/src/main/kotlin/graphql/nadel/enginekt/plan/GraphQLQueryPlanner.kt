package graphql.nadel.enginekt.plan

import graphql.nadel.Service
import graphql.nadel.enginekt.transform.result.GraphQLResultTransform
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

class GraphQLQueryPlanner(
    private val overallSchema: GraphQLSchema,
    private val resultTransforms: List<GraphQLResultTransform>,
) {
    fun generate(
        userContext: Any?,
        service: Service,
        field: NormalizedField,
    ): GraphQLQueryPlan {
        return GraphQLQueryPlan(
            getResultTransformsRecursively(userContext, service, field)
        )
    }

    private fun getResultTransformsRecursively(
        userContext: Any?,
        service: Service,
        field: NormalizedField,
    ): List<GraphQLResultTransformIntent> {
        return getResultTransforms(userContext, service, field) + field.children.flatMap {
            getResultTransformsRecursively(userContext, service, field = it)
        }
    }

    private fun getResultTransforms(
        userContext: Any?,
        service: Service,
        field: NormalizedField,
    ): List<GraphQLResultTransformIntent> {
        return resultTransforms.mapNotNull {
            if (it.isApplicable(userContext, overallSchema, service, field)) {
                GraphQLResultTransformIntent(service, field, it)
            } else {
                null
            }
        }
    }

    companion object {
        fun create(overallSchema: GraphQLSchema): GraphQLQueryPlanner {
            return GraphQLQueryPlanner(overallSchema, emptyList())
        }
    }
}
