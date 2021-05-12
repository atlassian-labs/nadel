package graphql.nadel.enginekt.transform.query.hydration

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.transform.query.NadelFieldInstructionTransform
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

internal class NadelBatchHydrationFieldTransform : NadelFieldInstructionTransform<NadelBatchHydrationFieldInstruction> {
    override fun transform(
        transformer: NadelQueryTransformer,
        service: Service,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        field: NormalizedField,
        instruction: NadelBatchHydrationFieldInstruction,
    ): List<NormalizedField> {
        TODO("Not yet implemented")
    }
}
