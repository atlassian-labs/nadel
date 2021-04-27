package graphql.nadel.enginekt.plan

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.GraphQLExecutionBlueprint
import graphql.nadel.enginekt.transform.result.GraphQLResultTransform
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema
import graphql.schema.FieldCoordinates.coordinates as createFieldCoordinates

class GraphQLExecutionPlanner(
    private val executionBlueprint: GraphQLExecutionBlueprint,
    private val overallSchema: GraphQLSchema,
    private val resultTransforms: List<GraphQLResultTransform>,
) {
    fun generate(
        userContext: Any?,
        service: Service,
        rootField: NormalizedField,
    ): GraphQLExecutionPlan {
        val schemaTransformations = mutableListOf<GraphQLSchemaTransformation>()
        val resultTransformations = mutableListOf<GraphQLResultTransformation>()

        traverseQueryTree(rootField) { field ->
            schemaTransformations += getSchemaTransformations(field)
            resultTransformations += getResultTransformations(userContext, service, field)
        }

        return GraphQLExecutionPlan(
            schemaTransformations.groupBy { it.field },
            resultTransformations.groupBy { it.field },
        )
    }

    private fun getSchemaTransformations(field: NormalizedField): List<GraphQLSchemaTransformation> {
        val coordinates = createFieldCoordinates(field.objectType.name, field.name)

        return listOfNotNull(
            when (val underlyingField = executionBlueprint.underlyingFields[coordinates]) {
                null -> null
                else -> GraphQLUnderlyingFieldTransformation(field, underlyingField)
            },
            when (val underlyingType = executionBlueprint.underlyingTypes[field.objectType.name]) {
                null -> null
                else -> GraphQLUnderlyingTypeTransformation(field, underlyingType)
            }
        )
    }

    private fun getResultTransformations(
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

    private fun traverseQueryTree(root: NormalizedField, consumer: (NormalizedField) -> Unit) {
        consumer(root)
        root.children.forEach {
            traverseQueryTree(it, consumer)
        }
    }

    companion object {
        fun create(
            executionBlueprint: GraphQLExecutionBlueprint,
            overallSchema: GraphQLSchema,
        ): GraphQLExecutionPlanner {
            return GraphQLExecutionPlanner(
                executionBlueprint,
                overallSchema,
                emptyList()
            )
        }
    }
}
