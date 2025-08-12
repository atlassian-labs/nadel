package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.transform.GraphQLObjectTypeName
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldContext
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformOperationContext
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.hydration.NadelHydrationFieldsBuilder
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationTransform.TransformFieldContext
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationTransform.TransformOperationContext
import graphql.nadel.engine.transform.makeTypeNameField
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField

internal class NadelBatchHydrationTransform(
    engine: NextgenEngine,
) : NadelTransform<TransformOperationContext, TransformFieldContext> {
    data class TransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
    ) : NadelTransformOperationContext()

    data class TransformFieldContext(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
        val instructionsByObjectTypeNames: Map<GraphQLObjectTypeName, List<NadelBatchHydrationFieldInstruction>>,
        val aliasHelper: NadelAliasHelper,
    ) : NadelTransformFieldContext<TransformOperationContext>() {
        val virtualField: ExecutableNormalizedField get() = overallField
        val virtualFieldService: Service get() = service
    }

    override suspend fun getTransformOperationContext(
        operationExecutionContext: NadelOperationExecutionContext,
    ): TransformOperationContext {
        return TransformOperationContext(operationExecutionContext)
    }

    private val newHydrator = NadelNewBatchHydrator(engine)

    override suspend fun getTransformFieldContext(
        transformContext: TransformOperationContext,
        overallField: ExecutableNormalizedField,
    ): TransformFieldContext? {
        val executionBlueprint = transformContext.executionBlueprint
        val hydrationDetails = transformContext.operationExecutionContext.hydrationDetails

        val instructionsByObjectTypeName = executionBlueprint
            .getInstructionInsideVirtualType<NadelBatchHydrationFieldInstruction>(hydrationDetails, overallField)
            .ifEmpty {
                executionBlueprint
                    .getTypeNameToInstructionsMap<NadelBatchHydrationFieldInstruction>(overallField)
            }

        return if (instructionsByObjectTypeName.isNotEmpty()) {
            return TransformFieldContext(
                parentContext = transformContext,
                overallField = overallField,
                instructionsByObjectTypeNames = instructionsByObjectTypeName,
                aliasHelper = NadelAliasHelper.forField(tag = "batch_hydration", overallField),
            )
        } else {
            null
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
                .flatMap { (objectTypeName, instructions) ->
                    NadelHydrationFieldsBuilder.makeRequiredSourceFields(
                        service = transformContext.virtualFieldService,
                        executionBlueprint = transformContext.executionBlueprint,
                        aliasHelper = transformContext.aliasHelper,
                        objectTypeName = objectTypeName,
                        instructions = instructions,
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

    override suspend fun transformResult(
        transformContext: TransformFieldContext,
        underlyingParentField: ExecutableNormalizedField?,
        resultNodes: JsonNodes,
    ): List<NadelResultInstruction> {
        val parentNodes = resultNodes.getNodesAt(
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )

        return newHydrator.hydrate(transformContext, parentNodes)
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
}
