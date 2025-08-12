package graphql.nadel.engine.transform.stub

import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.blueprint.NadelStubbedInstruction
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldContext
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformOperationContext
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.makeTypeNameField
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.transform.stub.NadelStubTransform.TransformFieldContext
import graphql.nadel.engine.transform.stub.NadelStubTransform.TransformOperationContext
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField

internal class NadelStubTransform : NadelTransform<TransformOperationContext, TransformFieldContext> {
    data class TransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
    ) : NadelTransformOperationContext()

    data class TransformFieldContext(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
        val stubByObjectTypeNames: Map<String, NadelStubbedInstruction>,
        val aliasHelper: NadelAliasHelper,
    ) : NadelTransformFieldContext<TransformOperationContext>()

    override suspend fun getTransformOperationContext(
        operationExecutionContext: NadelOperationExecutionContext,
    ): TransformOperationContext {
        return TransformOperationContext(operationExecutionContext)
    }

    override suspend fun getTransformFieldContext(
        transformContext: TransformOperationContext,
        overallField: ExecutableNormalizedField,
    ): TransformFieldContext? {
        val instructions =
            transformContext.executionBlueprint.getTypeNameToInstructionMap<NadelStubbedInstruction>(overallField)
                .ifEmpty { return null }

        return TransformFieldContext(
            parentContext = transformContext,
            overallField = overallField,
            stubByObjectTypeNames = instructions,
            aliasHelper = NadelAliasHelper.forField("stubbed", overallField),
        )
    }

    override suspend fun transformField(
        transformContext: TransformFieldContext,
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult {
        // When stubbing interface fields, we allow some implementations to be stubbed, other fields can be real impls
        val objectTypesWithRealFieldImplementations =
            field.objectTypeNames - transformContext.stubByObjectTypeNames.keys

        return NadelTransformFieldResult(
            newField = if (objectTypesWithRealFieldImplementations.isEmpty()) {
                null
            } else {
                field.toBuilder()
                    .clearObjectTypesNames()
                    .objectTypeNames(objectTypesWithRealFieldImplementations.toList())
                    .build()
            },
            artificialFields = listOfNotNull(
                makeTypeNameField(transformContext, field),
            ),
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

        return parentNodes.mapNotNull { parentNode ->
            val parentObject = parentNode.value as? JsonMap?
            if (parentObject == null) {
                null
            } else {
                val typename = parentObject[transformContext.aliasHelper.typeNameResultKey]
                if (typename == null) {
                    null
                } else {
                    stub(parentNode, transformContext.overallField)
                }
            }
        }
    }

    private fun stub(
        parentObject: JsonNode,
        overallField: ExecutableNormalizedField,
    ): NadelResultInstruction {
        return NadelResultInstruction.Set(
            subject = parentObject,
            field = overallField,
            newValue = null,
        )
    }

    private fun makeTypeNameField(
        state: TransformFieldContext,
        field: ExecutableNormalizedField,
    ): ExecutableNormalizedField? {
        val typeNamesWithInstructions = state.stubByObjectTypeNames.keys
        val objectTypeNames = field.objectTypeNames
            .filter { it in typeNamesWithInstructions }
            .ifEmpty { return null }

        return makeTypeNameField(
            aliasHelper = state.aliasHelper,
            objectTypeNames = objectTypeNames,
            deferredExecutions = linkedSetOf(),
        )
    }
}
