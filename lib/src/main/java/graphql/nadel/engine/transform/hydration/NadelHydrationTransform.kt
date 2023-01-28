package graphql.nadel.engine.transform.hydration

import graphql.nadel.NadelEngineContext
import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelEngineExecutionHooks
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.engine.blueprint.getTypeNameToInstructionsMap
import graphql.nadel.engine.blueprint.hydration.NadelHydrationStrategy
import graphql.nadel.engine.transform.GraphQLObjectTypeName
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformContext
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformUtil.makeTypeNameField
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.getInstructionsForNode
import graphql.nadel.engine.transform.hydration.NadelHydrationFieldsBuilder.makeRequiredJoiningFields
import graphql.nadel.engine.transform.hydration.NadelHydrationTransform.TransformContext
import graphql.nadel.engine.transform.hydration.NadelHydrationUtil.getInstructionsToAddErrors
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Would recommend a read of [NadelGenericHydrationInstruction] for more context.
 */
internal class NadelHydrationTransform(
    private val engine: NextgenEngine,
) : NadelTransform<TransformContext> {
    data class TransformContext(
        /**
         * The hydration instructions for the [hydrationCauseField]. There can be multiple instructions
         * as a [ExecutableNormalizedField] can have multiple [ExecutableNormalizedField.objectTypeNames].
         *
         * The [Map.Entry.key] of [FieldCoordinates] denotes a specific object type and
         * its associated instruction.
         */
        override val instructionsByObjectTypeNames: Map<GraphQLObjectTypeName, List<NadelHydrationFieldInstruction>>,
        override val hydrationCauseService: Service,
        /**
         * The field in question for the transform, stored for quick access when
         * the [TransformContext] is passed around.
         */
        override val hydrationCauseField: ExecutableNormalizedField,
        override val aliasHelper: NadelAliasHelper,
    ) : NadelTransformContext, NadelHydrationTransformContext

    context(NadelEngineContext, NadelExecutionContext)
    override suspend fun isApplicable(
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): TransformContext? {
        val hydrationInstructionsByTypeNames = executionBlueprint.fieldInstructions
            .getTypeNameToInstructionsMap<NadelHydrationFieldInstruction>(overallField)

        return if (hydrationInstructionsByTypeNames.isEmpty()) {
            null
        } else {
            TransformContext(
                instructionsByObjectTypeNames = hydrationInstructionsByTypeNames,
                hydrationCauseService = service,
                hydrationCauseField = overallField,
                aliasHelper = NadelAliasHelper.forField(tag = "hydration", overallField),
            )
        }
    }

    context(NadelEngineContext, NadelExecutionContext, TransformContext)
    override suspend fun transformField(
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult {
        val objectTypesNoRenames = field.objectTypeNames.filterNot { it in instructionsByObjectTypeNames }

        return NadelTransformFieldResult(
            newField = objectTypesNoRenames
                .takeIf(List<GraphQLObjectTypeName>::isNotEmpty)
                ?.let {
                    field.toBuilder()
                        .clearObjectTypesNames()
                        .objectTypeNames(it)
                        .build()
                },
            artificialFields = instructionsByObjectTypeNames
                .flatMap { (typeName, instructions) ->
                    makeRequiredJoiningFields(
                        objectTypeName = typeName,
                        instructions = instructions,
                    )
                }
                .let { fields ->
                    when (val typeNameField = makeTypeNameField(field)) {
                        null -> fields
                        else -> fields + typeNameField
                    }
                },
        )
    }

    context(TransformContext)
    private fun makeTypeNameField(
        field: ExecutableNormalizedField,
    ): ExecutableNormalizedField? {
        val typeNamesWithInstructions = instructionsByObjectTypeNames.keys
        val objectTypeNames = field.objectTypeNames
            .filter { it in typeNamesWithInstructions }
            .takeIf { it.isNotEmpty() }
            ?: return null

        return makeTypeNameField(
            aliasHelper = aliasHelper,
            objectTypeNames = objectTypeNames,
        )
    }

    context(NadelEngineContext, NadelExecutionContext, TransformContext)
    override suspend fun getResultInstructions(
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        val parentNodes = nodes.getNodesAt(
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )

        val jobs: List<Deferred<List<NadelResultInstruction>>> = coroutineScope {
            parentNodes.map {
                async {
                    hydrate(
                        parentNode = it,
                        fieldToHydrate = overallField,
                    )
                }
            }
        }

        return jobs.awaitAll().flatten()
    }

    context(NadelEngineContext, NadelExecutionContext, TransformContext)
    private suspend fun hydrate(
        parentNode: JsonNode,
        fieldToHydrate: ExecutableNormalizedField, // Field asking for hydration from the overall query
    ): List<NadelResultInstruction> {
        val instructions = instructionsByObjectTypeNames.getInstructionsForNode(
            executionBlueprint = executionBlueprint,
            service = hydrationCauseService,
            aliasHelper = aliasHelper,
            parentNode = parentNode,
        )

        // Do nothing if there is no hydration instruction associated with this result
        if (instructions.isEmpty()) {
            return emptyList()
        }

        val instruction = getHydrationFieldInstruction(instructions, parentNode)
            ?: return listOf(
                NadelResultInstruction.Set(
                    subject = parentNode,
                    key = NadelResultKey(hydrationCauseField.resultKey),
                    newValue = null,
                ),
            )

        val engineContext = this@NadelEngineContext
        val executionContext = this@NadelExecutionContext

        val actorQueryResults = coroutineScope {
            NadelHydrationFieldsBuilder.makeEffectQueries(
                instruction = instruction,
                parentNode = parentNode,
            ).map { actorQuery ->
                async {
                    val hydrationSourceService = executionBlueprint.getServiceOwning(instruction.location)!!
                    val hydrationActorField =
                        FieldCoordinates.coordinates(instruction.effectFieldContainer, instruction.effectFieldDef)
                    val serviceHydrationDetails = ServiceExecutionHydrationDetails(
                        timeout = instruction.timeout,
                        batchSize = 1,
                        hydrationCauseService = hydrationSourceService,
                        hydrationCauseField = instruction.location,
                        hydrationEffectField = hydrationActorField
                    )
                    engine.executeTopLevelField(
                        engineContext,
                        executionContext,
                        service = instruction.effectService,
                        topLevelField = actorQuery,
                        serviceHydrationDetails = serviceHydrationDetails,
                    )
                }
            }.awaitAll()
        }

        when (instruction.hydrationStrategy) {
            is NadelHydrationStrategy.OneToOne -> {
                // Should not have more than one query for one to one
                val result = actorQueryResults.emptyOrSingle()

                val data = result?.data?.let { data ->
                    JsonNodeExtractor.getNodesAt(
                        data = data,
                        queryPath = instruction.queryPathToEffectField,
                    ).emptyOrSingle()
                }

                val errors = result?.let(::getInstructionsToAddErrors) ?: emptyList()

                return listOf(
                    NadelResultInstruction.Set(
                        subject = parentNode,
                        key = NadelResultKey(fieldToHydrate.resultKey),
                        newValue = JsonNode(data?.value),
                    ),
                ) + errors
            }

            is NadelHydrationStrategy.ManyToOne -> {
                val data = actorQueryResults.map { result ->
                    JsonNodeExtractor.getNodesAt(
                        data = result.data,
                        queryPath = instruction.queryPathToEffectField,
                    ).emptyOrSingle()?.value
                }

                val addErrors = getInstructionsToAddErrors(actorQueryResults)

                return listOf(
                    NadelResultInstruction.Set(
                        subject = parentNode,
                        key = NadelResultKey(fieldToHydrate.resultKey),
                        newValue = JsonNode(data),
                    ),
                ) + addErrors
            }
        }
    }

    context(NadelEngineContext, NadelExecutionContext, TransformContext)
    private fun getHydrationFieldInstruction(
        instructions: List<NadelHydrationFieldInstruction>,
        parentNode: JsonNode,
    ): NadelHydrationFieldInstruction? {
        return when (instructions.size) {
            1 -> instructions.single()
            else -> {
                if (serviceExecutionHooks is NadelEngineExecutionHooks) {
                    serviceExecutionHooks.getHydrationInstruction(
                        instructions,
                        parentNode,
                        aliasHelper,
                        userContext
                    )
                } else {
                    error(
                        "Cannot decide which hydration instruction should be used. Provided ServiceExecutionHooks has " +
                            "to be of type NadelEngineExecutionHooks"
                    )
                }
            }
        }
    }
}
