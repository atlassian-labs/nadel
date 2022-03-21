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
import graphql.nadel.engine.transform.result.json.JsonNodes
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
        val typeNameNodes = nodes.getNodesAt(
            (underlyingParentField?.queryPath ?: NadelQueryPath.root) + overallField.resultKey,
            flatten = true,
        )

        return typeNameNodes.mapNotNull { typeNameNode ->
            val underlyingTypeName = typeNameNode.value as String?
                ?: return@mapNotNull null
            val overallTypeName = executionBlueprint.getOverallTypeName(
                service = service,
                underlyingTypeName = underlyingTypeName,
            )
            NadelResultInstruction.Set(
                subjectPath = typeNameNode.resultPath,
                newValue = overallTypeName,
            )
        }
    }
}
