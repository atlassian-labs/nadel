package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.util.resolveObjectTypes
import graphql.nadel.engine.util.unwrapAll
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedField.newNormalizedField
import graphql.schema.GraphQLOutputType

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
    fun makeObjectIdFields(
        executionBlueprint: NadelOverallExecutionBlueprint,
        aliasHelper: NadelAliasHelper,
        batchHydrationInstruction: NadelBatchHydrationFieldInstruction,
    ): List<ExecutableNormalizedField> {
        return when (val matchStrategy = batchHydrationInstruction.batchHydrationMatchStrategy) {
            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifier -> makeObjectIdFields(
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

    private fun makeObjectIdFields(
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
            overallParentTypeOfIdField = batchHydrationInstruction.actorFieldDef.type,
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
        overallParentTypeOfIdField: GraphQLOutputType,
    ): List<String> {
        val overallTypeName = overallParentTypeOfIdField.unwrapAll().name
        val overallType = executionBlueprint.engineSchema.getType(overallTypeName)
            ?: error("Could not find the overall output type for the actor field")

        return resolveObjectTypes(
            schema = executionBlueprint.engineSchema,
            type = overallType,
            onNotObjectType = {
                errorForUnsupportedObjectIdParentType()
            },
        ).map { it.name }
    }

    private fun errorForUnsupportedObjectIdParentType(): Nothing {
        error("When matching by object identifier, the output type of actor field must be an object")
    }
}
