package graphql.nadel.enginekt.transform.hydration.batch

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.util.resolveObjectTypes
import graphql.nadel.enginekt.util.unwrapAll
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedField.newNormalizedField
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLType

/**
 * Builds the field used to identify objects returned by batch hydration e.g.
 *
 * ```graphql
 * type Query {
 *   issues(ids: [ID]): [Issue]
 * }
 *
 * type Issue {
 *   id: ID
 *   relatedIssueIds: [ID]
 *   key: String
 *
 *   relatedIssues: [Issue] @hydrated(
 *      service: "IssueService"
 *      field: "issues"
 *      arguments : [{ name: "ids" value: "$source.relatedIssueIds" }]
 *      identifiedBy: "id"
 *      batchSize: 2
 *   )
 * }
 * ```
 *
 * And given a hydration query
 *
 * ```graphql
 * query {
 *   issues(ids: ["PROJ-1", "PROJ-5", "PROJ-3"]) {
 *      key
 *   }
 * }
 * ```
 *
 * We need an id to actually identify the objects to know where to insert them back
 * into the original result.
 *
 * i.e. this code inserts the selection for the `id` field
 */
internal object NadelBatchHydrationObjectIdFieldBuilder {
    fun makeObjectIdField(
        executionBlueprint: NadelOverallExecutionBlueprint,
        aliasHelper: NadelAliasHelper,
        batchHydrationInstruction: NadelBatchHydrationFieldInstruction,
    ): List<ExecutableNormalizedField> {
        return when (val matchStrategy = batchHydrationInstruction.batchHydrationMatchStrategy) {
            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifier -> makeObjectIdField(
                executionBlueprint,
                aliasHelper,
                batchHydrationInstruction,
                matchStrategy,
            )
            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers -> makeObjectIdFields(
                executionBlueprint,
                aliasHelper,
                batchHydrationInstruction,
                matchStrategy.objectIds,
            )
            else -> emptyList()
        }
    }

    private fun makeObjectIdField(
        executionBlueprint: NadelOverallExecutionBlueprint,
        aliasHelper: NadelAliasHelper,
        batchHydrationInstruction: NadelBatchHydrationFieldInstruction,
        matchStrategy: NadelBatchHydrationMatchStrategy.MatchObjectIdentifier,
    ): List<ExecutableNormalizedField> {
        return makeObjectIdFields(
            executionBlueprint,
            aliasHelper,
            batchHydrationInstruction,
            listOf(matchStrategy),
        )
    }

    private fun makeObjectIdFields(
        executionBlueprint: NadelOverallExecutionBlueprint,
        aliasHelper: NadelAliasHelper,
        batchHydrationInstruction: NadelBatchHydrationFieldInstruction,
        objectIds: List<NadelBatchHydrationMatchStrategy.MatchObjectIdentifier>,
    ): List<ExecutableNormalizedField> {
        val objectTypeNames = getObjectTypeNamesForIdField(
            executionBlueprint = executionBlueprint,
            actorService = batchHydrationInstruction.actorService,
            underlyingParentTypeOfIdField = batchHydrationInstruction.actorFieldDef.type,
        )

        return objectIds
            .map { objectId ->
                newNormalizedField()
                    .objectTypeNames(objectTypeNames)
                    .fieldName(objectId.resultId)
                    .alias(aliasHelper.getResultKey(objectId.resultId))
                    .build()
            }
    }

    /**
     * Gets the type names for the parent of the object id field. Note that this
     * must be the OVERALL type names.
     */
    private fun getObjectTypeNamesForIdField(
        executionBlueprint: NadelOverallExecutionBlueprint,
        actorService: Service,
        underlyingParentTypeOfIdField: GraphQLOutputType,
    ): List<String> {
        val overallType = getUnwrappedOverallType(
            executionBlueprint = executionBlueprint,
            service = actorService,
            underlyingType = underlyingParentTypeOfIdField,
        ) ?: error("Could not find the overall output type for the actor field")

        return resolveObjectTypes(
            schema = executionBlueprint.engineSchema,
            type = overallType,
            onNotObjectType = {
                errorForUnsupportedObjectIdParentType()
            },
        ).map { it.name }
    }

    /**
     * NOTE: the hydrated field output type must ALWAYS be exposed, otherwise it isn't a valid hydration.
     * We use this function to get the overall output type of the hydrated field.
     */
    private fun getUnwrappedOverallType(
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        underlyingType: GraphQLType,
    ): GraphQLType? {
        val underlyingTypeName = underlyingType.unwrapAll().name
        val overallTypeName = executionBlueprint.getOverallTypeName(service, underlyingTypeName)
        return executionBlueprint.engineSchema.getType(overallTypeName)
    }

    private fun errorForUnsupportedObjectIdParentType(): Nothing {
        error("When matching by object identifier, the output type of actor field must be an object")
    }
}
