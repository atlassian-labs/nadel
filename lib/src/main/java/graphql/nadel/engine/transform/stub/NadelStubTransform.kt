package graphql.nadel.engine.transform.stub

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.blueprint.NadelStubbedInstruction
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformServiceExecutionContext
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.makeTypeNameField
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.transform.stub.NadelStubTransform.StubState
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField

internal class NadelStubTransform : NadelTransform<StubState> {
    data class StubState(
        val stubByObjectTypeNames: Map<String, NadelStubbedInstruction>,
        val aliasHelper: NadelAliasHelper,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): StubState? {
        val instructions = executionBlueprint.getTypeNameToInstructionMap<NadelStubbedInstruction>(overallField)
            .ifEmpty { return null }

        return StubState(
            stubByObjectTypeNames = instructions,
            aliasHelper = NadelAliasHelper.forField("stubbed", overallField),
        )
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: StubState,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
    ): NadelTransformFieldResult {
        // When stubbing interface fields, we allow some implementations to be stubbed, other fields can be real impls
        val objectTypesNamesWithoutStubbed = field.objectTypeNames.filter { it !in state.stubByObjectTypeNames }

        return NadelTransformFieldResult(
            newField = if (objectTypesNamesWithoutStubbed.isNotEmpty()) {
                field.toBuilder()
                    .clearObjectTypesNames()
                    .objectTypeNames(objectTypesNamesWithoutStubbed)
                    .build()
            } else {
                null
            },
            artificialFields = listOfNotNull(
                makeTypeNameField(state, field),
            ),
        )
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: StubState,
        nodes: JsonNodes,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
    ): List<NadelResultInstruction> {
        val parentNodes = nodes.getNodesAt(
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )

        return parentNodes.mapNotNull { parentNode ->
            val parentObject = parentNode.value as? JsonMap?
            if (parentObject == null) {
                null
            } else {
                val typename = parentObject[state.aliasHelper.typeNameResultKey]
                if (typename == null) {
                    null
                } else {
                    stub(parentNode, overallField)
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
        state: StubState,
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
