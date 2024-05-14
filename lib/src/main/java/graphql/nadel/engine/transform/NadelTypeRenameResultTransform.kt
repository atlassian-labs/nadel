package graphql.nadel.engine.transform

import graphql.introspection.Introspection
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTypeRenameResultTransform.State
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.queryPath
import graphql.normalized.ExecutableNormalizedField

internal class NadelTypeRenameResultTransform : NadelTransform<State> {
    data class State(
        val typeRenamePath: NadelQueryPath,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {
        return if (overallField.fieldName == Introspection.TypeNameMetaFieldDef.name) {
            State(
                typeRenamePath = overallField.queryPath,
            )
        } else {
            null
        }
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: State,
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
        state: State,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        val parentNodes = nodes.getNodesAt(
            underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )

        return parentNodes.mapNotNull { parentNode ->
            @Suppress("UNCHECKED_CAST")
            val parentMap = parentNode.value as? JsonMap
                ?: return@mapNotNull null
            val underlyingTypeName = parentMap[overallField.resultKey] as String?
                ?: return@mapNotNull null

            val overallTypeName = executionBlueprint.getOverallTypeName(
                service = service,
                underlyingTypeName = underlyingTypeName,
            )

            val typeName: String = if (executionContext.hints.sharedTypeRenames(service)) {
                if (overallField.objectTypeNames.contains(overallTypeName)) {
                    overallTypeName
                } else {
                    overallField.objectTypeNames.singleOrNull {
                        executionBlueprint.getRename(it)?.underlyingName == underlyingTypeName
                    } ?: overallTypeName
                }
            } else {
                overallTypeName
            }
            NadelResultInstruction.Set(
                subject = parentNode,
                key = NadelResultKey(overallField.resultKey),
                newValue = JsonNode(typeName),
            )
        }
    }
}
