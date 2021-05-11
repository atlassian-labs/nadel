package graphql.nadel.enginekt.transform.query.deepRename

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelDeepRenameFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.transform.query.NadelPathToField
import graphql.nadel.enginekt.transform.query.NadelFieldInstructionTransform
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

internal class NadelDeepRenameFieldTransform : NadelFieldInstructionTransform<NadelDeepRenameFieldInstruction> {
    override fun transform(
        transformer: NadelQueryTransformer,
        service: Service,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        field: NormalizedField,
        instruction: NadelDeepRenameFieldInstruction,
    ): List<NormalizedField> {
        return listOf(
            createDeepField(
                transformer,
                executionBlueprint,
                service,
                field,
                deepRename = instruction,
            )
        )
    }

    private fun createDeepField(
        transformer: NadelQueryTransformer,
        blueprint: NadelExecutionBlueprint,
        service: Service,
        field: NormalizedField,
        deepRename: NadelDeepRenameFieldInstruction,
    ): NormalizedField {
        val underlyingTypeName = field.objectType.name.let { overallTypeName ->
            blueprint.typeInstructions[overallTypeName]?.underlyingName ?: overallTypeName
        }

        val underlyingObjectType = service.underlyingSchema.getObjectType(underlyingTypeName)
            ?: error("No underlying object type")

        return NadelPathToField.createField(
            schema = service.underlyingSchema,
            parentType = underlyingObjectType,
            pathToField = deepRename.pathToSourceField,
            fieldChildren = transformer.transformFields(service, field.children),
        )
    }
}
