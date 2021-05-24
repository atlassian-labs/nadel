package graphql.nadel.enginekt.transform.query

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.blueprint.NadelFieldInstruction
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

internal interface NadelQueryTransform<Instruction : NadelFieldInstruction> {
    fun transform(
            transformer: NadelQueryTransformer,
            service: Service,
            overallSchema: GraphQLSchema,
            executionBlueprint: NadelExecutionBlueprint,
            field: NormalizedField,
            instruction: Instruction,
    ): List<NormalizedField>
}
