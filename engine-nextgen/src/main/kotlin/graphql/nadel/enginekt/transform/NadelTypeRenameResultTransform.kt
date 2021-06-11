package graphql.nadel.enginekt.transform

import graphql.introspection.Introspection
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.query.QueryPath
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.queryPath
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

internal class NadelTypeRenameResultTransform : NadelTransform<NadelTypeRenameResultTransform.State> {
    data class State(
        val typeRenamePath: QueryPath,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: NormalizedField,
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
        transformer: NadelQueryTransformer.Continuation,
        service: Service,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelOverallExecutionBlueprint,
        field: NormalizedField,
        state: State,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult.unmodified(field)
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: NormalizedField,
        underlyingParentField: NormalizedField?,
        result: ServiceExecutionResult,
        state: State,
    ): List<NadelResultInstruction> {
        val typeNameNodes = JsonNodeExtractor.getNodesAt(
            result.data,
            state.typeRenamePath,
            flatten = true,
        )

        return typeNameNodes.map { typeNameNode ->
            val underlyingTypeName = typeNameNode.value as String
            val overallTypeName = executionBlueprint.getOverallTypeName(
                service = service,
                underlyingTypeName = underlyingTypeName,
            )
            NadelResultInstruction.Set(typeNameNode.resultPath, overallTypeName)
        }
    }
}
