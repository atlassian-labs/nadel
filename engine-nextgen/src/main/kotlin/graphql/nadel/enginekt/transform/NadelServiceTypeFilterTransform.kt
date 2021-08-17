package graphql.nadel.enginekt.transform

import graphql.language.ObjectTypeDefinition
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.IntrospectionService
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.NadelServiceTypeFilterTransform.State
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.util.queryPath
import graphql.nadel.enginekt.util.toBuilder
import graphql.normalized.ExecutableNormalizedField

class NadelServiceTypeFilterTransform : NadelTransform<State> {
    data class State(
        val typesOwnedByService: List<String>,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
    ): State? {
        when {
            // Ignore top level fields, they won't belong to multiple services
            overallField.parent == null -> return null
            // Introspection - do nothing. INTROSPECTION REALLY SHOULDN'T BE EXECUTING THIS CODE PATH
            service.name == IntrospectionService.name -> return null
        }

        // All types are owned by service
        val objectTypesOwnedByService = service.definitionRegistry
            .definitions
            .asSequence()
            .filterIsInstance<ObjectTypeDefinition>()
            .map { it.name }
            .toSet()

        val typesOwnedByService = overallField.objectTypeNames
            .filter { it in objectTypesOwnedByService }

        // All types are owned by service
        if (typesOwnedByService.size == overallField.objectTypeNames.size) {
            return null
        }

        return State(typesOwnedByService)
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer.Continuation,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: State,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult(
            newField = field
                .toBuilder()
                .clearObjectTypesNames()
                .objectTypeNames(state.typesOwnedByService)
                .children(transformer.transform(field.children))
                .build(),
        )
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: State,
    ): List<NadelResultInstruction> {
        return emptyList()
    }
}
