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
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField

internal data class NadelStubTransformOperationContext(
    override val parentContext: NadelOperationExecutionContext,
) : NadelTransformOperationContext()

internal data class NadelStubTransformFieldContext(
    override val parentContext: NadelStubTransformOperationContext,
    override val overallField: ExecutableNormalizedField,
    val stubByObjectTypeNames: Map<String, NadelStubbedInstruction>,
    val aliasHelper: NadelAliasHelper,
) : NadelTransformFieldContext<NadelStubTransformOperationContext>()

internal class NadelStubTransform : NadelTransform<
    NadelStubTransformOperationContext,
    NadelStubTransformFieldContext
    > {
    override suspend fun getTransformOperationContext(
        operationExecutionContext: NadelOperationExecutionContext,
    ): NadelStubTransformOperationContext {
        return NadelStubTransformOperationContext(operationExecutionContext)
    }

    override suspend fun getTransformFieldContext(
        transformContext: NadelStubTransformOperationContext,
        overallField: ExecutableNormalizedField,
    ): NadelStubTransformFieldContext? {
        val instructions =
            transformContext.executionBlueprint.getTypeNameToInstructionMap<NadelStubbedInstruction>(overallField)
                .ifEmpty { return null }

        return NadelStubTransformFieldContext(
            parentContext = transformContext,
            overallField = overallField,
            stubByObjectTypeNames = instructions,
            aliasHelper = NadelAliasHelper.forField("stubbed", overallField),
        )
    }

    override suspend fun transformField(
        transformContext: NadelStubTransformFieldContext,
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
        transformContext: NadelStubTransformFieldContext,
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
        state: NadelStubTransformFieldContext,
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
