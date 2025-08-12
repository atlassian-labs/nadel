package graphql.nadel.engine.transform.hydration

import graphql.incremental.DeferPayload
import graphql.incremental.DelayedIncrementalPartialResultImpl
import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelHydrationStrategy
import graphql.nadel.engine.transform.GraphQLObjectTypeName
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldContext
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformOperationContext
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.getInstructionsForNode
import graphql.nadel.engine.transform.hydration.NadelHydrationTransform.TransformFieldContext
import graphql.nadel.engine.transform.hydration.NadelHydrationTransform.TransformOperationContext
import graphql.nadel.engine.transform.makeTypeNameField
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.toBuilder
import graphql.nadel.engine.util.toGraphQLError
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Would recommend a read of [NadelGenericHydrationInstruction] for more context.
 */
internal class NadelHydrationTransform(
    private val engine: NextgenEngine,
) : NadelTransform<TransformOperationContext, TransformFieldContext> {
    data class TransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
    ) : NadelTransformOperationContext()

    data class TransformFieldContext(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
        /**
         * The hydration instructions for the [virtualField]. There can be multiple instructions
         * as a [ExecutableNormalizedField] can have multiple [ExecutableNormalizedField.objectTypeNames].
         *
         * The [Map.Entry.key] of [FieldCoordinates] denotes a specific object type and
         * its associated instruction.
         */
        val instructionsByObjectTypeNames: Map<String, List<NadelHydrationFieldInstruction>>,
        val aliasHelper: NadelAliasHelper,
    ) : NadelTransformFieldContext<TransformOperationContext>() {
        val virtualFieldService: Service get() = service
        val virtualField: ExecutableNormalizedField get() = overallField
    }

    override suspend fun getTransformOperationContext(
        operationExecutionContext: NadelOperationExecutionContext,
    ): TransformOperationContext {
        return TransformOperationContext(operationExecutionContext)
    }

    override suspend fun getTransformFieldContext(
        transformContext: TransformOperationContext,
        overallField: ExecutableNormalizedField,
    ): TransformFieldContext? {
        val executionBlueprint = transformContext.executionBlueprint
        val hydrationDetails = transformContext.operationExecutionContext.hydrationDetails

        val instructionsByObjectTypeName = executionBlueprint
            .getInstructionInsideVirtualType<NadelHydrationFieldInstruction>(hydrationDetails, overallField)
            .ifEmpty {
                executionBlueprint
                    .getTypeNameToInstructionsMap<NadelHydrationFieldInstruction>(overallField)
            }

        return if (instructionsByObjectTypeName.isEmpty()) {
            null
        } else {
            TransformFieldContext(
                parentContext = transformContext,
                overallField = overallField,
                instructionsByObjectTypeNames = instructionsByObjectTypeName,
                aliasHelper = NadelAliasHelper.forField(tag = "hydration", overallField),
            )
        }
    }

    override suspend fun transformField(
        transformContext: TransformFieldContext,
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult {
        val objectTypesNoRenames =
            field.objectTypeNames.filterNot { it in transformContext.instructionsByObjectTypeNames }

        return NadelTransformFieldResult(
            newField = objectTypesNoRenames
                .takeIf(List<GraphQLObjectTypeName>::isNotEmpty)
                ?.let {
                    field.toBuilder()
                        .clearObjectTypesNames()
                        .objectTypeNames(it)
                        .build()
                },
            artificialFields = transformContext.instructionsByObjectTypeNames
                .flatMap { (typeName, instruction) ->
                    NadelHydrationFieldsBuilder.makeRequiredSourceFields(
                        service = transformContext.service,
                        executionBlueprint = transformContext.executionBlueprint,
                        aliasHelper = transformContext.aliasHelper,
                        objectTypeName = typeName,
                        instructions = instruction,
                    )
                }
                .let { fields ->
                    when (val typeNameField = makeTypeNameField(transformContext, field)) {
                        null -> fields
                        else -> fields + typeNameField
                    }
                },
        )
    }

    private fun makeTypeNameField(
        transformContext: TransformFieldContext,
        field: ExecutableNormalizedField,
    ): ExecutableNormalizedField? {
        val typeNamesWithInstructions = transformContext.instructionsByObjectTypeNames.keys
        val objectTypeNames = field.objectTypeNames
            .filter { it in typeNamesWithInstructions }
            .takeIf { it.isNotEmpty() }
            ?: return null

        return makeTypeNameField(
            aliasHelper = transformContext.aliasHelper,
            objectTypeNames = objectTypeNames,
            deferredExecutions = linkedSetOf(),
        )
    }

    override suspend fun transformResult(
        transformContext: TransformFieldContext,
        underlyingParentField: ExecutableNormalizedField?,
        resultNodes: JsonNodes,
    ): List<NadelResultInstruction> {
        val parentNodes = resultNodes.getNodesAt(
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )

        return if (isDeferred(transformContext)) {
            deferHydration(parentNodes, transformContext)
            return emptyList()
        } else {
            getResultInstructions(parentNodes, transformContext)
        }
    }

    private suspend fun getResultInstructions(
        parentNodes: List<JsonNode>,
        transformContext: TransformFieldContext,
    ): List<NadelResultInstruction> {
        val overallField = transformContext.overallField

        return coroutineScope {
            parentNodes
                .mapNotNull {
                    prepareHydration(
                        parentNode = it,
                        transformContext = transformContext,
                    )
                }
                .map {
                    async {
                        it.hydrate()
                    }
                }
                .awaitAll()
                .flatMap { hydration ->
                    val setData = sequenceOf(
                        NadelResultInstruction.Set(
                            subject = hydration.parentNode,
                            newValue = hydration.newValue,
                            field = overallField,
                        ),
                    )
                    val addErrors = hydration.errors
                        .asSequence()
                        .filterNotNull()
                        .map { error ->
                            toGraphQLError(error)
                        }
                        .map {
                            NadelResultInstruction.AddError(it)
                        }

                    setData + addErrors
                }
        }
    }

    private fun deferHydration(
        parentNodes: List<JsonNode>,
        transformContext: TransformFieldContext,
    ) {
        val overallField = transformContext.overallField
        val executionContext = transformContext.executionContext

        // Prepare the hydrations before we go async
        // We need to do this because if we run it async below, we cannot guarantee that our artificial fields have not yet been removed
        val preparedHydrations = parentNodes
            .mapNotNull {
                prepareHydration(
                    parentNode = it,
                    transformContext = transformContext,
                )
            }

        if (preparedHydrations.isEmpty()) {
            return
        }

        // TODO: Extract to Utils somewhere
        // This isn't really right… but we start with this
        val label = overallField.deferredExecutions.firstNotNullOfOrNull { it.label }

        executionContext.incrementalResultSupport.defer {
            val hydrations = preparedHydrations
                .map {
                    async {
                        it.hydrate()
                    }
                }
                .awaitAll()

            DelayedIncrementalPartialResultImpl.Builder()
                .incrementalItems(
                    hydrations
                        .map { hydration -> // Hydration of one parent node
                            val data = hydration.newValue

                            val parentPath = executionContext.resultTracker.getResultPath(
                                overallField.queryPath.dropLast(1),
                                hydration.parentNode,
                            )!!
                            val path = parentPath + overallField.resultKey

                            DeferPayload.newDeferredItem()
                                .label(label)
                                .data(
                                    mapOf(
                                        overallField.resultKey to data?.value,
                                    ),
                                )
                                .path(parentPath.toRawPath())
                                .errors(
                                    hydration.errors
                                        .asSequence()
                                        .filterNotNull()
                                        .map {
                                            toGraphQLError(
                                                raw = it,
                                                path = path.toRawPath(),
                                            )
                                        }
                                        .toList(),
                                )
                                .build()
                        }
                )
                .build()
        }
    }

    private fun prepareHydration(
        parentNode: JsonNode,
        transformContext: TransformFieldContext,
    ): NadelPreparedHydration? {
        val executionBlueprint = transformContext.executionBlueprint
        val virtualField = transformContext.virtualField
        val executionContext = transformContext.executionContext

        val instructions = transformContext.instructionsByObjectTypeNames.getInstructionsForNode(
            executionBlueprint = transformContext.executionBlueprint,
            service = transformContext.virtualFieldService,
            aliasHelper = transformContext.aliasHelper,
            parentNode = parentNode,
        )

        // Do nothing if there is no hydration instruction associated with this result
        if (instructions.isEmpty()) {
            return null
        }

        val instruction = getHydrationFieldInstruction(transformContext, instructions, parentNode)
            ?: return NadelPreparedHydration {
                NadelHydrationResult(
                    parentNode = parentNode,
                    newValue = null,
                    errors = emptyList(),
                )
            }

        val backingQueries = NadelHydrationFieldsBuilder.makeBackingQueries(
            executionContext = transformContext.executionContext,
            service = transformContext.virtualFieldService,
            aliasHelper = transformContext.aliasHelper,
            virtualField = transformContext.virtualField,
            parentNode = parentNode,
            instruction = instruction,
        )

        return NadelPreparedHydration {
            val backingQueryResults = coroutineScope {
                backingQueries
                    .map { backingQuery ->
                        async {
                            val hydrationSourceService = executionBlueprint.getServiceOwning(instruction.location)!!
                            val hydrationBackingField =
                                FieldCoordinates.coordinates(
                                    instruction.backingFieldContainer,
                                    instruction.backingFieldDef
                                )
                            val serviceHydrationDetails = ServiceExecutionHydrationDetails(
                                instruction = instruction,
                                timeout = instruction.timeout,
                                batchSize = 1,
                                hydrationSourceService = hydrationSourceService,
                                hydrationVirtualField = instruction.location,
                                hydrationBackingField = hydrationBackingField,
                                fieldPath = virtualField.listOfResultKeys,
                            )
                            engine.executeHydration(
                                service = instruction.backingService,
                                topLevelField = backingQuery,
                                executionContext = executionContext,
                                hydrationDetails = serviceHydrationDetails,
                            )
                        }
                    }.awaitAll()
            }

            when (instruction.hydrationStrategy) {
                is NadelHydrationStrategy.OneToOne -> {
                    // Should not have more than one query for one to one
                    val result = backingQueryResults.emptyOrSingle()

                    val data = result?.data?.let { data ->
                        JsonNodeExtractor.getNodesAt(
                            data = data,
                            queryPath = instruction.queryPathToBackingField,
                        ).emptyOrSingle()
                    }

                    NadelHydrationResult(
                        parentNode = parentNode,
                        newValue = JsonNode(data?.value),
                        errors = result?.errors ?: emptyList(),
                    )
                }
                is NadelHydrationStrategy.ManyToOne -> {
                    val data = backingQueryResults
                        .map { result ->
                            JsonNodeExtractor.getNodesAt(
                                data = result.data,
                                queryPath = instruction.queryPathToBackingField,
                            ).emptyOrSingle()?.value
                        }

                    NadelHydrationResult(
                        parentNode = parentNode,
                        newValue = JsonNode(data),
                        errors = backingQueryResults.flatMap { it.errors },
                    )
                }
            }
        }
    }

    private fun getHydrationFieldInstruction(
        transformContext: TransformFieldContext,
        instructions: List<NadelHydrationFieldInstruction>,
        parentNode: JsonNode,
    ): NadelHydrationFieldInstruction? {
        if (instructions.any { it.condition == null }) {
            return transformContext.executionContext.hooks.getHydrationInstruction(
                virtualField = transformContext.virtualField,
                instructions = instructions,
                parentNode = parentNode,
                aliasHelper = transformContext.aliasHelper,
                userContext = transformContext.executionContext.userContext,
            )
        }

        return instructions
            .firstOrNull {
                // Note: due to the validation, all instructions in here have a condition, so can call explicitly
                val resultQueryPath = transformContext.aliasHelper.getQueryPath(it.condition!!.fieldPath)
                val node = JsonNodeExtractor.getNodesAt(parentNode, resultQueryPath)
                    .emptyOrSingle()
                it.condition.evaluate(node?.value)
            }
    }

    private fun isDeferred(
        transformContext: TransformFieldContext,
    ): Boolean {
        // Disable defer in nested hydration
        if (transformContext.operationExecutionContext.hydrationDetails != null) {
            return false
        }

        return transformContext.executionContext.hints.deferSupport()
            && transformContext.overallField.deferredExecutions.isNotEmpty()
    }
}

/**
 * A prepared hydration is a hydration that is ready to run, and not dependent on any result objects etc.
 *
 * This exists because hydrations can be `@defer`red.
 *
 * If you defer the hydration code, some values in the result may not be available as they will have been mutated.
 *
 * e.g. for this hydration
 *
 * ``` graphql
 * type Issue {
 *   assignee: User
 *     @hydrated(
 *       service: "users"
 *       field: "user"
 *       arguments: [{name: "id", value: "$source.assigneeId"}]
 *     )
 * }
 * ```
 *
 * Then we need the value of `assigneeId` in the result, but this is an artificial field.
 * Nadel removes artificial fields before the result gets sent back to the caller.
 * So we "prepare" a hydration to ensure we have the value of the artificial field before it gets removed.
 */
private fun interface NadelPreparedHydration {
    suspend fun hydrate(): NadelHydrationResult
}

private data class NadelHydrationResult(
    val parentNode: JsonNode,
    val newValue: JsonNode?,
    val errors: List<JsonMap?>,
)
