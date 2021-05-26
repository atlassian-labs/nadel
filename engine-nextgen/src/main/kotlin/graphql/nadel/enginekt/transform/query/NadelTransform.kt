package graphql.nadel.enginekt.transform.query

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

internal interface NadelTransform<State : Any> {

    fun isApplicable(
        userContext: Any?,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        service: Service,
        field: NormalizedField,
    ): State?

    // NF => List<NF>,
    // transforms from overall into underlying
    fun transformField(
        transformer: NadelQueryTransformerContinue,
        service: Service,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        field: NormalizedField,
        state: State,
    ): List<NormalizedField>

    fun getResultInstructions(
        userContext: Any?,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        service: Service,
        field: NormalizedField,
        result: ServiceExecutionResult,
        state: State,
    ): List<NadelResultInstruction>
}
