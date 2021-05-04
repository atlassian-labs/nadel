package graphql.nadel.enginekt.plan

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.transform.result.NadelResultTransform
import graphql.nadel.enginekt.transform.result.deepRename.NadelDeepRenameResultTransform
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema
import graphql.schema.FieldCoordinates.coordinates as createFieldCoordinates

internal class NadelExecutionPlanFactory(
    private val executionBlueprint: NadelExecutionBlueprint,
    private val overallSchema: GraphQLSchema,
    private val resultTransforms: List<NadelResultTransform>,
) {
    /**
     * This derives an execution plan from with the main input parameters being the
     * [rootField] and [executionBlueprint].
     */
    fun create(
        userContext: Any?,
        service: Service,
        rootField: NormalizedField,
    ): NadelExecutionPlan {
        val schemaTransformations = mutableListOf<NadelSchemaTransformation>()
        val resultTransformations = mutableListOf<GraphQLResultTransformation>()

        traverseQueryTree(rootField) { field ->
            schemaTransformations += getSchemaTransformations(field)
            resultTransformations += getResultTransformations(userContext, service, field)
        }

        return NadelExecutionPlan(
            schemaTransformations.groupBy { it.field },
            resultTransformations.groupBy { it.field },
        )
    }

    private fun getSchemaTransformations(field: NormalizedField): List<NadelSchemaTransformation> {
        val coordinates = createFieldCoordinates(field.objectType.name, field.name)

        return listOfNotNull(
            when (val underlyingField = executionBlueprint.underlyingFields[coordinates]) {
                null -> null
                else -> NadelUnderlyingFieldTransformation(field, underlyingField)
            },
            when (val underlyingType = executionBlueprint.underlyingTypes[field.objectType.name]) {
                null -> null
                else -> NadelUnderlyingTypeTransformation(field, underlyingType)
            }
        )
    }

    private fun getResultTransformations(
        userContext: Any?,
        service: Service,
        field: NormalizedField,
    ): List<GraphQLResultTransformation> {
        return resultTransforms.mapNotNull {
            if (it.isApplicable(userContext, overallSchema, executionBlueprint, service, field)) {
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
            executionBlueprint: NadelExecutionBlueprint,
            overallSchema: GraphQLSchema,
        ): NadelExecutionPlanFactory {
            return NadelExecutionPlanFactory(
                executionBlueprint,
                overallSchema,
                resultTransforms = listOf(
                    NadelDeepRenameResultTransform(),
                ),
            )
        }
    }
}
