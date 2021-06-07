package graphql.nadel.enginekt.transform

import graphql.introspection.Introspection
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.query.QueryPath
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

internal class NadelTypeRenameResultTransform : NadelTransform<NadelTypeRenameResultTransform.State> {
    data class State(
        val typeRenamePath: QueryPath,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        field: NormalizedField,
    ): State? {
        return if (field.fieldName == Introspection.TypeNameMetaFieldDef.name) {
            State(
                typeRenamePath = QueryPath(field.listOfResultKeys),
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
        executionPlan: NadelExecutionPlan,
        field: NormalizedField,
        state: State,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult.unmodified(field)
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        overallSchema: GraphQLSchema,
        executionPlan: NadelExecutionPlan,
        service: Service,
        field: NormalizedField,
        result: ServiceExecutionResult,
        state: State,
    ): List<NadelResultInstruction> {
        val nodes = JsonNodeExtractor.getNodesAt(
            result.data,
            state.typeRenamePath.segments,
            flatten = true,
        )

        return nodes.map {
            val underlyingTypeName = it.value as String
            val overallTypeName = executionPlan.getOverallTypeName(underlyingTypeName)
            NadelResultInstruction.Set(it.resultPath, overallTypeName)
        }
    }
}
