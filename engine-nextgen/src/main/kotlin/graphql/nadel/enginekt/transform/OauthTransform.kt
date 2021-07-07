package graphql.nadel.enginekt.transform

import graphql.GraphQLError
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
import graphql.normalized.ExecutableNormalizedField
import graphql.validation.ValidationError
import graphql.validation.ValidationErrorType

class OauthTransform : NadelTransform<GraphQLError> {

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField
    ): GraphQLError? {
        if (overallField.objectTypeNames
                .map { executionBlueprint.schema.getType(it) }
                .any { it == null }
        ) {
            return null
        }

        if (overallField.getOneFieldDefinition(executionBlueprint.schema)
                .getDirective("scopes") != null
        ) {
            return ValidationError(ValidationErrorType.WrongType)
        }
        return null
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer.Continuation,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: GraphQLError
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult(
            newField = null,
            artificialFields = listOf(
                ExecutableNormalizedField.newNormalizedField()
                    .level(field.level)
                    .objectTypeNames(listOf(field.objectTypeNames.iterator().next()))
                    .fieldName(Introspection.TypeNameMetaFieldDef.name)
                    .parent(field.parent)
                    .alias("uuid_typename")
                    .build()
            )
        )
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: GraphQLError
    ): List<NadelResultInstruction> {
        val nodesAt = JsonNodeExtractor.getNodesAt(
            data = result.data,
            queryPath = underlyingParentField?.queryPath ?: QueryPath.root,
            flatten = true,
        )

        return nodesAt.map { parentNode ->
            val destinationPath = parentNode.resultPath + overallField.resultKey
            NadelResultInstruction.Set(
                subjectPath = destinationPath,
                newValue = null,
            )
        } + NadelResultInstruction.AddError(state)
    }
}
