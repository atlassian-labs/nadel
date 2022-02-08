package graphql.nadel.enginekt.transform

import graphql.introspection.Introspection
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.IntrospectionService
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.NadelServiceTypeFilterTransform.State
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNodes
import graphql.nadel.enginekt.util.resolveObjectTypes
import graphql.nadel.enginekt.util.toBuilder
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedField.newNormalizedField

/**
 * Nadel ends up building a complex schema from multiple different services. This class
 * ensures that a query going to a specific service doesn't get types from another service.
 * You may think this never happens but with shared types this becomes a possibility.
 *
 * e.g. given an overall schema
 *
 * ```graphql
 * service shared {
 *   interface Error { id: ID }
 * }
 * service A {
 *   type Query {
 *      aErrors: [Error]
 *   }
 *   type AError implements Error { id: ID }
 * }
 * service B {
 *   type BError implements Error {
 *     id: ID
 *     b: String
 *   }
 * }
 * ```
 *
 * A given query is completely valid:
 *
 * ```graphql
 * query {
 *   aErrors {
 *     ... on BError {
 *       id
 *     }
 *   }
 * }
 * ```
 *
 * But `aErrors` goes to the `A` service where the type `BError` doesn't exist.
 *
 * See the tests for more examples.
 *
 * - service-types-are-filtered.yml
 * - service-types-are-completely-filtered.yml
 */
class NadelServiceTypeFilterTransform : NadelTransform<State> {
    data class State(
        val aliasHelper: NadelAliasHelper,
        val typeNamesOwnedByService: Set<String>,
        val fieldObjectTypeNamesOwnedByService: List<String>,
        val overallField: ExecutableNormalizedField,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {
        when {
            // Ignore top level fields, they won't belong to multiple services
            // Do not randomly remove this, we rely on this later on too.
            // This transform never runs for top level fields
            overallField.parent == null -> return null
            // Introspection - do nothing. INTROSPECTION REALLY SHOULDN'T BE EXECUTING THIS CODE PATH
            service.name == IntrospectionService.name -> return null
        }

        val typeNamesOwnedByService = getTypeNamesServiceOwns(executionBlueprint, service)
        // Add underlying type names as well to handle combination of hydration and renames.
        // Transforms are applied to hydration fields as well, and those fields always reference
        // elements from the underlying schema
        val underlyingTypeNamesOwnedByService =
            getUnderlyingTypeNamesServiceOwns(executionBlueprint, service, typeNamesOwnedByService)

        val allTypeNamesOwnedByService = typeNamesOwnedByService + underlyingTypeNamesOwnedByService

        val fieldObjectTypeNamesOwnedByService = overallField.objectTypeNames
            .filter { it in allTypeNamesOwnedByService }

        // All types are owned by service
        // Note: one list is a subset of the other, so if size is same, contents are too
        if (fieldObjectTypeNamesOwnedByService.size == overallField.objectTypeNames.size) {
            return null
        }


        return State(
            aliasHelper = NadelAliasHelper.forField(
                tag = "type_filter",
                field = overallField,
            ),
            typeNamesOwnedByService = typeNamesOwnedByService,
            fieldObjectTypeNamesOwnedByService = fieldObjectTypeNamesOwnedByService,
            overallField = overallField,
        )
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: State,
    ): NadelTransformFieldResult {
        // Nothing to query if there are no fields, we need to add selection
        if (state.fieldObjectTypeNamesOwnedByService.isEmpty()) {
            val objectTypeNames = state.overallField.parent.getFieldDefinitions(executionBlueprint.schema)
                .asSequence()
                .flatMap { fieldDef ->
                    resolveObjectTypes(
                        executionBlueprint.schema,
                        type = fieldDef.type,
                        onNotObjectType = { type ->
                            error("Unable to resolve to object type: $type")
                        }
                    )
                }
                .map {
                    it.name
                }
                .filter {
                    it in state.typeNamesOwnedByService
                }
                .toSet()
                .toList()

            if (objectTypeNames.isEmpty()) {
                error("Service does not own return type. Unable to insert __typename as schema is not configured properly.")
            }

            return NadelTransformFieldResult(
                newField = null,
                artificialFields = listOf(
                    newNormalizedField()
                        .objectTypeNames(objectTypeNames)
                        .alias(state.aliasHelper.typeNameResultKey)
                        .fieldName(Introspection.TypeNameMetaFieldDef.name)
                        .build(),
                ),
            )
        }

        return NadelTransformFieldResult(
            newField = field
                .toBuilder()
                .clearObjectTypesNames()
                .objectTypeNames(state.fieldObjectTypeNamesOwnedByService)
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
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        return emptyList()
    }

    /**
     * Gets the type names a service owns. These are the overall type names.
     *
     * __WARNING:__
     *
     * These also include types that are not exposed, so use this list carefully.
     *
     * Only use this to create subsets of OTHER lists. Never use this list directly.
     */
    private fun getTypeNamesServiceOwns(
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
    ): Set<String> {
        return service.underlyingSchema
            .typeMap
            .values
            .asSequence()
            .map {
                executionBlueprint.getOverallTypeName(service, it.name)
            }
            .toSet()
    }

    private fun getUnderlyingTypeNamesServiceOwns(
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallTypeNames: Set<String>
    ): Set<String> {
        return overallTypeNames
            .asSequence()
            .map {
                executionBlueprint.getUnderlyingTypeName(service, it)
            }
            .toSet()
    }
}
