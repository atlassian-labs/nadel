package graphql.nadel.enginekt.transform

import graphql.language.ObjectTypeDefinition
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.normalized.ExecutableNormalizedField

internal class NadelBlahBlahTransform : NadelTransform<NadelBlahBlahTransform.State> {
    data class State (
        val typesNotOwnedByService: List<String>
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField
    ): State? {

        service.definitionRegistry
            .getDefinitions(ObjectTypeDefinition::class.java)



    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer.Continuation,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: State
    ): NadelTransformFieldResult {


        val newTypeNames = field.objectTypeNames.filter {
            it != "Issue"
        }

        NadelTransformFieldResult(
            newField = field.transform { builder -> builder.objectTypeNames(newTypeNames) }
        )
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

