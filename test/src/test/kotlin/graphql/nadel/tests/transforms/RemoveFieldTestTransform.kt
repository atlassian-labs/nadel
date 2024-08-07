package graphql.nadel.tests.transforms

import graphql.GraphQLError
import graphql.introspection.Introspection
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.queryPath
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLObjectType
import graphql.validation.ValidationError.newValidationError
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
                executionBlueprint.engineSchema.getType(it) as GraphQLObjectType?
            }
            .filterNotNull()
            .firstOrNull()
            ?: return null

        if (objectType.getField(overallField.name)?.getDirective("toBeDeleted") != null) {
            return newValidationError().validationErrorType(ValidationErrorType.WrongType).build()
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
        state: GraphQLError,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        val parentNodes = nodes.getNodesAt(
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )
        return parentNodes.map { parentNode ->
            NadelResultInstruction.Set(
                subject = parentNode,
                key = NadelResultKey(overallField.resultKey),
                newValue = null,
            )
        } + NadelResultInstruction.AddError(state)
    }
}
