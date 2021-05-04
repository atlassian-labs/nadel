package graphql.nadel.enginekt.transform.query.deepRename

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelDeepRenameInstruction
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.transform.query.NadelPathToField.getField
import graphql.nadel.enginekt.transform.query.NadelQueryTransform
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

class NadelDeepRenameQueryTransform : NadelQueryTransform<NadelDeepRenameInstruction> {
    override fun transform(
        service: Service,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        field: NormalizedField,
        instruction: NadelDeepRenameInstruction,
    ): List<NormalizedField> {
        return listOf(
            makeDeepSelection(
                service,
                field,
                deepRename = instruction,
            )
        )
    }

    private fun makeDeepSelection(
        service: Service,
        field: NormalizedField,
        deepRename: NadelDeepRenameInstruction,
    ): NormalizedField {
        return getField(
            schema = service.underlyingSchema,
            parentType = field.objectType,
            pathToSourceField = deepRename.pathToSourceField,
            sourceFieldChildren = field.children,
        )
    }
}
