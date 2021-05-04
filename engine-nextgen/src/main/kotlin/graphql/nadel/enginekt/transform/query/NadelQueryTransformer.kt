package graphql.nadel.enginekt.transform.query

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationInstruction
import graphql.nadel.enginekt.blueprint.NadelDeepRenameInstruction
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.blueprint.NadelHydrationInstruction
import graphql.nadel.enginekt.transform.query.deepRename.NadelDeepRenameQueryTransform
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema
import graphql.schema.FieldCoordinates.coordinates as makeFieldCoordinates

internal class NadelQueryTransformer(
    private val overallSchema: GraphQLSchema,
    private val executionBlueprint: NadelExecutionBlueprint,
    private val deepRenameTransform: NadelDeepRenameQueryTransform,
) {
    fun transform(
        userContext: Any?,
        service: Service,
        field: NormalizedField,
    ): List<NormalizedField> {
        val fieldCoordinates = makeFieldCoordinates(field.objectType, field.fieldDefinition)

        return when (val fieldInstruction = executionBlueprint.fieldInstructions[fieldCoordinates]) {
            is NadelHydrationInstruction -> TODO()
            is NadelBatchHydrationInstruction -> TODO()
            is NadelDeepRenameInstruction -> deepRenameTransform.transform(
                service,
                overallSchema,
                executionBlueprint,
                field,
                fieldInstruction,
            )
            null -> listOf(field)
        }
    }

    companion object {
        fun create(overallSchema: GraphQLSchema, executionBlueprint: NadelExecutionBlueprint): NadelQueryTransformer {
            return NadelQueryTransformer(
                overallSchema,
                executionBlueprint,
                deepRenameTransform = NadelDeepRenameQueryTransform(),
            )
        }
    }
}
