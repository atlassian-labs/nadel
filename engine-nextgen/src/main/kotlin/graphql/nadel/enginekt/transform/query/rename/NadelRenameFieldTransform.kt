package graphql.nadel.enginekt.transform.query.rename

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.blueprint.NadelRenameFieldInstruction
import graphql.nadel.enginekt.transform.query.NadelFieldInstructionTransform
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

internal class NadelRenameFieldTransform : NadelFieldInstructionTransform<NadelRenameFieldInstruction> {
    override fun transform(
        transformer: NadelQueryTransformer,
        service: Service,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        field: NormalizedField,
        instruction: NadelRenameFieldInstruction,
    ): List<NormalizedField> {
        TODO("Not yet implemented")
    }
}
