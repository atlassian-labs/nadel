package graphql.nadel.tests.transforms

import graphql.GraphQLError
import graphql.introspection.Introspection
import graphql.nadel.NadelEngineContext
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformState
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.queryPath
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLObjectType
import graphql.validation.ValidationError.newValidationError
import graphql.validation.ValidationErrorType

class RemoveFieldTestTransform : NadelTransform<RemoveFieldTestTransform.State> {
    data class State(val error: GraphQLError) : NadelTransformState

    context(NadelEngineContext, NadelExecutionContext)
    override suspend fun isApplicable(
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {
        val objectType = overallField.objectTypeNames.asSequence()
            .map {
                executionBlueprint.engineSchema.getType(it) as GraphQLObjectType?
            }
            .filterNotNull()
            .firstOrNull()
            ?: return null

        if (objectType.getField(overallField.name)?.getDirective("toBeDeleted") != null) {
            return State(
                newValidationError()
                    .validationErrorType(ValidationErrorType.WrongType)
                    .build(),
            )
        }

        return null
    }

    context(NadelEngineContext, NadelExecutionContext, State)
    override suspend fun transformField(
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
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

    context(NadelEngineContext, NadelExecutionContext, State)
    override suspend fun getResultInstructions(
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        val parentNodes = JsonNodeExtractor.getNodesAt(
            data = result.data,
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )

        return parentNodes.map { parentNode ->
            NadelResultInstruction.Set(
                subject = parentNode,
                key = NadelResultKey(overallField.resultKey),
                newValue = null,
            )
        } + NadelResultInstruction.AddError(error)
    }
}
