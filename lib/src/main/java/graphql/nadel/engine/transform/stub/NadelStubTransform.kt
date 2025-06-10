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
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLDirectiveContainer

class NadelStubTransform : NadelTransform<StubState> {
    data class StubState(
        // todo: replace with other
        val stubByObjectTypeNames: Map<String, NadelStubbedInstruction?>,
        // val stubByObjectTypeNames: Map<String, NadelStubbedInstruction>,
        val aliasHelper: NadelAliasHelper,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
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
    ): NadelTransformFieldResult {
        val remainingObjectTypeNames = field.objectTypeNames - state.stubByObjectTypeNames.keys

        return NadelTransformFieldResult(
            newField = if (remainingObjectTypeNames.isEmpty()) {
                null
            } else {
                field.toBuilder()
                    .clearObjectTypesNames()
                    .objectTypeNames(remainingObjectTypeNames.toList())
                    .build()
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

data class NadelStubDirective(
    private val appliedDirective: GraphQLAppliedDirective,
) {
    companion object {
        const val directiveName = "stubbed"
    }
}

fun GraphQLDirectiveContainer.hasStubbedDirective(): Boolean {
    return hasAppliedDirective(NadelStubDirective.directiveName)
}
