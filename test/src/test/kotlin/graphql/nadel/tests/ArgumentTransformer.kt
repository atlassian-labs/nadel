package graphql.nadel.tests

import graphql.language.StringValue
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue
import java.util.function.Consumer

internal class ArgumentTransformer: NadelTransform<ArgumentTransformer.State> {
    data class State(
        val x: String
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField
    ): State? {
        return if(overallField.name == "comment") State(x="X") else null
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer.Continuation,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: State
    ): NadelTransformFieldResult {
        val transformedField: ExecutableNormalizedField =
            field.transform(Consumer { builder: ExecutableNormalizedField.Builder ->
                builder.normalizedArguments(mapOf("id" to NormalizedInputValue("ID!", StringValue.newStringValue("comment-transformed:1").build())))
            })

        transformer.transform(field.children)
        return NadelTransformFieldResult(transformedField)
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: State
    ): List<NadelResultInstruction> {
        return emptyList()
    }
}