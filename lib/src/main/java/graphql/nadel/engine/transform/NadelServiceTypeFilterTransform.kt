package graphql.nadel.engine.transform

import graphql.introspection.Introspection
import graphql.nadel.NadelEngineContext
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.IntrospectionService
import graphql.nadel.engine.transform.NadelServiceTypeFilterTransform.TransformContext
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.resolveObjectTypes
import graphql.nadel.engine.util.toBuilder
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
class NadelServiceTypeFilterTransform : NadelTransform<TransformContext> {
    data class TransformContext(
        val aliasHelper: NadelAliasHelper,
        val typeNamesOwnedByService: Set<String>,
        val fieldObjectTypeNamesOwnedByService: List<String>,
        val overallField: ExecutableNormalizedField,
    ) : NadelTransformContext

    context(NadelEngineContext, NadelExecutionContext)
    override suspend fun isApplicable(
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): TransformContext? {
        when {
            // Ignore top level fields, they won't belong to multiple services
            // Do not randomly remove this, we rely on this later on too.
            // This transform never runs for top level fields
            overallField.parent == null -> return null
            // Introspection - do nothing. INTROSPECTION REALLY SHOULDN'T BE EXECUTING THIS CODE PATH
            service.name == IntrospectionService.name -> return null
        }

        val typeNamesOwnedByService = executionBlueprint.getOverAllTypeNamesForService(service)
        // Add underlying type names as well to handle combination of hydration and renames.
        // Transforms are applied to hydration fields as well, and those fields always reference
        // elements from the underlying schema
        val underlyingTypeNamesOwnedByService = executionBlueprint.getUnderlyingTypeNamesForService(service)

        val fieldObjectTypeNamesOwnedByService = overallField.objectTypeNames
            .filter {
                // it is MUCH quicker to compare membership in 2 sets rather than
                // concat 1 giant set and then check
                it in typeNamesOwnedByService || it in underlyingTypeNamesOwnedByService
            }

        // All types are owned by service
        // Note: one list is a subset of the other, so if size is same, contents are too
        if (fieldObjectTypeNamesOwnedByService.size == overallField.objectTypeNames.size) {
            return null
        }


        return TransformContext(
            aliasHelper = NadelAliasHelper.forField(
                tag = "type_filter",
                field = overallField,
            ),
            typeNamesOwnedByService = typeNamesOwnedByService,
            fieldObjectTypeNamesOwnedByService = fieldObjectTypeNamesOwnedByService,
            overallField = overallField,
        )
    }

    context(NadelEngineContext, NadelExecutionContext, TransformContext)
    override suspend fun transformField(
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult {
        // Nothing to query if there are no fields, we need to add selection
        if (fieldObjectTypeNamesOwnedByService.isEmpty()) {
            val objectTypeNames = overallField.parent.getFieldDefinitions(executionBlueprint.engineSchema)
                .asSequence()
                .flatMap { fieldDef ->
                    resolveObjectTypes(
                        executionBlueprint.engineSchema,
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
                    it in typeNamesOwnedByService
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
                        .alias(aliasHelper.typeNameResultKey)
                        .fieldName(Introspection.TypeNameMetaFieldDef.name)
                        .build(),
                ),
            )
        }

        return NadelTransformFieldResult(
            newField = field
                .toBuilder()
                .clearObjectTypesNames()
                .objectTypeNames(fieldObjectTypeNamesOwnedByService)
                .build(),
        )
    }

    context(NadelEngineContext, NadelExecutionContext, TransformContext)
    override suspend fun getResultInstructions(
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        return emptyList()
    }
}
