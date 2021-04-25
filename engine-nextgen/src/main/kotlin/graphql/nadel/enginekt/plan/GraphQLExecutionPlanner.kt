package graphql.nadel.enginekt.plan

import graphql.nadel.Service
import graphql.nadel.enginekt.transform.result.GraphQLResultTransform
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

class GraphQLExecutionPlanner(
    private val overallSchema: GraphQLSchema,
    private val resultTransforms: List<GraphQLResultTransform>,
) {
    fun generate(
        userContext: Any?,
        service: Service,
        field: NormalizedField,
    ): GraphQLExecutionPlan {
        return GraphQLExecutionPlan(
            emptyList(),
            getResultTransformsRecursively(userContext, service, field),
        )
    }

    private fun getResultTransformsRecursively(
        userContext: Any?,
        service: Service,
        field: NormalizedField,
    ): List<GraphQLResultTransformation> {
        return getResultTransforms(userContext, service, field) + field.children.flatMap {
            getResultTransformsRecursively(userContext, service, field = it)
        }
    }

    private fun getResultTransforms(
        userContext: Any?,
        service: Service,
        field: NormalizedField,
    ): List<GraphQLResultTransformation> {
        return resultTransforms.mapNotNull {
            if (it.isApplicable(userContext, overallSchema, service, field)) {
                GraphQLResultTransformation(service, field, it)
            } else {
                null
            }
        }
    }

    companion object {
        fun create(overallSchema: GraphQLSchema): GraphQLExecutionPlanner {
            return GraphQLExecutionPlanner(overallSchema, emptyList())
        }
    }
}
