package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.NadelEngineContext
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.util.resolveObjectTypes
import graphql.nadel.engine.util.unwrapAll
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedField.newNormalizedField
import graphql.schema.GraphQLOutputType
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationTransform.NadelBatchHydrationContext as BatchTransformContext

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
    context(NadelEngineContext, NadelExecutionContext, BatchTransformContext)
    fun makeObjectIdFields(
        batchHydrationInstruction: NadelBatchHydrationFieldInstruction,
    ): List<ExecutableNormalizedField> {
        return when (val matchStrategy = batchHydrationInstruction.batchHydrationMatchStrategy) {
            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifier -> makeObjectIdFields(
                batchHydrationInstruction,
                matchStrategy,
            )

            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers -> makeObjectIdFields(
                batchHydrationInstruction,
                matchStrategy.objectIds,
            )

            else -> emptyList()
        }
    }

    context(NadelEngineContext, NadelExecutionContext, BatchTransformContext)
    private fun makeObjectIdFields(
        batchHydrationInstruction: NadelBatchHydrationFieldInstruction,
        matchStrategy: NadelBatchHydrationMatchStrategy.MatchObjectIdentifier,
    ): List<ExecutableNormalizedField> {
        return makeObjectIdFields(
            batchHydrationInstruction,
            listOf(matchStrategy),
        )
    }

    context(NadelEngineContext, NadelExecutionContext, BatchTransformContext)
    private fun makeObjectIdFields(
        batchHydrationInstruction: NadelBatchHydrationFieldInstruction,
        objectIds: List<NadelBatchHydrationMatchStrategy.MatchObjectIdentifier>,
    ): List<ExecutableNormalizedField> {
        val objectTypeNames = getObjectTypeNamesForIdField(
            overallParentTypeOfIdField = batchHydrationInstruction.effectFieldDef.type,
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
    context(NadelEngineContext, NadelExecutionContext, BatchTransformContext)
    private fun getObjectTypeNamesForIdField(
        overallParentTypeOfIdField: GraphQLOutputType,
    ): List<String> {
        val overallTypeName = overallParentTypeOfIdField.unwrapAll().name
        val overallType = overallSchema.getType(overallTypeName)
            ?: error("Could not find the overall output type for the effect field")

        return resolveObjectTypes(
            schema = overallSchema,
            type = overallType,
            onNotObjectType = {
                errorForUnsupportedObjectIdParentType()
            },
        ).map { it.name }
    }

    private fun errorForUnsupportedObjectIdParentType(): Nothing {
        error("When matching by object identifier, the output type of effect field must be an object")
    }
}
