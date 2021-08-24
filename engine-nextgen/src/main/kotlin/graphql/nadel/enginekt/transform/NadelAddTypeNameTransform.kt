package graphql.nadel.enginekt.transform

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLTypeUtil
import graphql.schema.GraphQLUnionType

class NadelAddTypeNameTransform : NadelTransform<NadelAliasHelper> {

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField
    ): NadelAliasHelper? {
        if (overallField.objectTypeNames
                .map { executionBlueprint.schema.getType(it) }
                .any { it == null }
        ) {
            return null
        }

        val type = GraphQLTypeUtil.unwrapAll(overallField.getType(executionBlueprint.schema))
        val isInterfaceOrUnion = type is GraphQLInterfaceType || type is GraphQLUnionType

        return when {
            isInterfaceOrUnion -> {
                NadelAliasHelper.forField("typename_hint", overallField)
            }
            else -> null
        }
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer.Continuation,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: NadelAliasHelper
    ): NadelTransformFieldResult {
        val graphQLOutputType = GraphQLTypeUtil.unwrapAll(field.getType(executionBlueprint.schema))
        val typeNames = graphQLOutputType.let {
            when (it) {
                is GraphQLInterfaceType -> {
                    return@let executionBlueprint.schema
                        .getImplementations(it)
                        .map { type -> type.name }
                }
                is GraphQLUnionType -> {
                    it.types.map { type -> type.name }
                }
                else -> {
                    error("should never happen: transform is only applicable to interfaces and unions")
                }
            }
        }

        return NadelTransformFieldResult(field, listOf(NadelTransformUtil.makeTypeNameField(state, typeNames)))
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: NadelAliasHelper
    ): List<NadelResultInstruction> {
        return emptyList()
    }
}
