package graphql.nadel.tests.transforms

import graphql.GraphQLError
import graphql.introspection.Introspection
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.query.NadelQueryPath
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.transform.result.json.JsonNodes
import graphql.nadel.enginekt.util.queryPath
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLObjectType
import graphql.validation.ValidationError
import graphql.validation.ValidationErrorType

class RemoveFieldTestTransform : NadelTransform<GraphQLError> {
    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): GraphQLError? {
        val objectType = overallField.objectTypeNames.asSequence()
            .map {
                executionBlueprint.schema.getType(it) as GraphQLObjectType?
            }
            .filterNotNull()
            .firstOrNull()
            ?: return null

        if (objectType.getField(overallField.name)?.getDirective("toBeDeleted") != null) {
            return ValidationError(ValidationErrorType.WrongType)
        }

        return null
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: GraphQLError,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult(
            newField = null,
            artificialFields = listOf(
                ExecutableNormalizedField.newNormalizedField()
                    .level(field.level)
                    .objectTypeNames(field.objectTypeNames.toList())
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
        state: GraphQLError,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        val parentNodes = JsonNodeExtractor.getNodesAt(
            data = result.data,
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )

        return parentNodes.map { parentNode ->
            val destinationPath = parentNode.resultPath + overallField.resultKey
            NadelResultInstruction.Set(
                subjectPath = destinationPath,
                newValue = null,
            )
        } + NadelResultInstruction.AddError(state)
    }
}
