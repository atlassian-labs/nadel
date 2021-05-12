package graphql.nadel.enginekt.transform.query.hydration

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.transform.query.NadelFieldInstructionTransform
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

internal class NadelHydrationFieldTransform : NadelFieldInstructionTransform<NadelHydrationFieldInstruction> {
    override fun transform(
        transformer: NadelQueryTransformer,
        service: Service,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        field: NormalizedField,
        instruction: NadelHydrationFieldInstruction,
    ): List<NormalizedField> {
        TODO("Not yet implemented")
    }
}
