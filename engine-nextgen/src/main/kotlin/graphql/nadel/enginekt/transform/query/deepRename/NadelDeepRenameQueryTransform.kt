package graphql.nadel.enginekt.transform.query.deepRename

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelDeepRenameFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.transform.query.NadelPathToField
import graphql.nadel.enginekt.transform.query.NadelQueryTransform
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

internal class NadelDeepRenameQueryTransform : NadelQueryTransform<NadelDeepRenameFieldInstruction> {
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
                service,
                field,
                deepRename = instruction,
            )
        )
    }

    private fun createDeepField(
        transformer: NadelQueryTransformer,
        service: Service,
        field: NormalizedField,
        deepRename: NadelDeepRenameFieldInstruction,
    ): NormalizedField {
        return NadelPathToField.createField(
            schema = service.underlyingSchema,
            parentType = field.objectType,
            pathToSourceField = deepRename.pathToSourceField,
            sourceFieldChildren = field.children.flatMap { child ->
                transformer.transform(service, field = child)
            },
        )
    }
}
