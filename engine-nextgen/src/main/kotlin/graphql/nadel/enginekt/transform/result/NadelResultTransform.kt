package graphql.nadel.enginekt.transform.result

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

interface NadelResultTransform {
    fun isApplicable(
        userContext: Any?,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        service: Service,
        field: NormalizedField,
    ): Boolean

    fun transform(
        userContext: Any?,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        service: Service,
        field: NormalizedField,
    ): List<NadelResultInstruction>
}
