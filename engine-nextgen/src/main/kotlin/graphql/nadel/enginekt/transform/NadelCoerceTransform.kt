package graphql.nadel.enginekt.transform

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.NadelCoerceTransform.State
import graphql.nadel.enginekt.transform.query.NadelQueryPath.Companion.root
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor.getNodesAt
import graphql.nadel.enginekt.util.queryPath
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLTypeUtil

internal class NadelCoerceTransform : NadelTransform<State> {
    data class State(
        val fieldType: GraphQLScalarType,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField
    ): State? {
        val schema = executionBlueprint.schema

        val types = overallField.objectTypeNames
            .asSequence()
            .mapNotNull {
                schema.getFieldDefinition(
                    FieldCoordinates.coordinates(
                        it,
                        overallField.fieldName
                    )
                )
            }
            .map { it.type }
            .toList()

        if (types.distinct().size != 1) {
            // field type on all definitions should be the same
            return null
        }

        val type = GraphQLTypeUtil.unwrapAll(types.first())

        if (GraphQLTypeUtil.isScalar(type)) {
            return State(type as GraphQLScalarType)
        }

        return null
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer.Continuation,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: State
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult.unmodified(field)
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: State
    ): List<NadelResultInstruction> {
        val parentQueryPath = underlyingParentField?.queryPath ?: root

        val valueNodes: List<JsonNode> = getNodesAt(
            data = result.data,
            queryPath = parentQueryPath.plus(overallField.resultKey),
            flatten = true
        )

        return valueNodes
            .mapNotNull { valueNode ->
                JsonNodeExtractor.getNodeAt(result.data, valueNode.resultPath)?.let { jsonNode ->
                    NadelResultInstruction.Set(
                        valueNode.resultPath,
                        jsonNode.value?.let { value -> state.fieldType.coercing.parseValue(value) })
                }
            }
    }
}