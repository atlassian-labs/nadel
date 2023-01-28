package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.NadelEngineContext
import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.getTypeNameToInstructionsMap
import graphql.nadel.engine.transform.GraphQLObjectTypeName
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformContext
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformUtil.makeTypeNameField
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.hydration.NadelHydrationFieldsBuilder.makeJoiningFields
import graphql.nadel.engine.transform.hydration.NadelHydrationTransformContext
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationTransform.TransformContext
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField

internal class NadelBatchHydrationTransform(
    engine: NextgenEngine,
) : NadelTransform<TransformContext> {
    private val hydrator = NadelBatchHydrator(engine)

    data class TransformContext(
        override val instructionsByObjectTypeNames: Map<GraphQLObjectTypeName, List<NadelBatchHydrationFieldInstruction>>,
        override val hydrationCauseField: ExecutableNormalizedField,
        override val hydrationCauseService: Service,
        override val aliasHelper: NadelAliasHelper,
    ) : NadelTransformContext, NadelHydrationTransformContext

    context(NadelEngineContext, NadelExecutionContext)
    override suspend fun isApplicable(
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): TransformContext? {
        val instructionsByObjectTypeName = executionBlueprint.fieldInstructions
            .getTypeNameToInstructionsMap<NadelBatchHydrationFieldInstruction>(overallField)

        return if (instructionsByObjectTypeName.isNotEmpty()) {
            return TransformContext(
                instructionsByObjectTypeNames = instructionsByObjectTypeName,
                hydrationCauseField = overallField,
                hydrationCauseService = service,
                aliasHelper = NadelAliasHelper.forField(tag = "batch_hydration", overallField),
            )
        } else {
            null
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
                .flatMap { (objectTypeName, instructions) ->
                    makeJoiningFields(
                        objectTypeName = objectTypeName,
                        instructions = instructions
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

        return hydrator.hydrate(parentNodes)
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
}
