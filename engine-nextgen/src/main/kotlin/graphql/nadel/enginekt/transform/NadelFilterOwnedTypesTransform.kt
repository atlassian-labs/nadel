package graphql.nadel.enginekt.transform

import graphql.language.ObjectTypeDefinition
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.normalized.ExecutableNormalizedField

internal class NadelFilterOwnedTypesTransform : NadelTransform<NadelFilterOwnedTypesTransform.State> {
    data class State(
        val typesNotOwnedByService: Set<String>
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField
    ): State? {
        if (overallField.parent == null) {
            return null;
        }

        val objectTypesOwnedByService = service.definitionRegistry
            .definitions
            .filterIsInstance<ObjectTypeDefinition>()
            .map { it.name }

        val typesNotOwnedByService = overallField.objectTypeNames - objectTypesOwnedByService

        if (typesNotOwnedByService.isEmpty()) {
            return null
        }

        return State(typesNotOwnedByService)
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
            !state.typesNotOwnedByService.contains(it)
        }

        val transformedChildren = transformer.transform(field.children)

        return NadelTransformFieldResult(
            newField = field.transform {
                it.clearObjectTypesNames()
                    .objectTypeNames(newTypeNames)
                    .children(transformedChildren)
            }
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

