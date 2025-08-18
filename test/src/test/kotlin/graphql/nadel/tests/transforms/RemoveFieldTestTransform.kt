package graphql.nadel.tests.transforms

import graphql.ErrorType
import graphql.GraphQLError
import graphql.introspection.Introspection
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldContext
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformOperationContext
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.newGraphQLError
import graphql.nadel.engine.util.queryPath
import graphql.nadel.tests.transforms.RemoveFieldTestTransform.TransformFieldContext
import graphql.nadel.tests.transforms.RemoveFieldTestTransform.TransformOperationContext
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLObjectType

class RemoveFieldTestTransform : NadelTransform<TransformOperationContext, TransformFieldContext> {
    data class TransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
    ) : NadelTransformOperationContext()

    data class TransformFieldContext(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
        val error: GraphQLError,
    ) : NadelTransformFieldContext<TransformOperationContext>()

    override suspend fun getTransformOperationContext(
        operationExecutionContext: NadelOperationExecutionContext,
    ): TransformOperationContext {
        return TransformOperationContext(operationExecutionContext)
    }

    override suspend fun getTransformFieldContext(
        transformContext: TransformOperationContext,
        overallField: ExecutableNormalizedField,
    ): TransformFieldContext? {
        val objectType = overallField.objectTypeNames.asSequence()
            .map {
                transformContext.executionBlueprint.engineSchema.getType(it) as GraphQLObjectType?
            }
            .filterNotNull()
            .firstOrNull()
            ?: return null

        if (objectType.getField(overallField.name)?.getDirective("toBeDeleted") != null) {
            return TransformFieldContext(
                parentContext = transformContext,
                overallField = overallField,
                error = newGraphQLError(
                    "field `${objectType.name}.${overallField.name}` has been removed by RemoveFieldTestTransform",
                    ErrorType.DataFetchingException,
                ),
            )
        }

        return null
    }

    override suspend fun transformField(
        transformContext: TransformFieldContext,
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

    override suspend fun transformResult(
        transformContext: TransformFieldContext,
        underlyingParentField: ExecutableNormalizedField?,
        resultNodes: JsonNodes,
    ): List<NadelResultInstruction> {
        val overallField = transformContext.overallField
        val parentNodes = resultNodes.getNodesAt(
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )

        return parentNodes.map { parentNode ->
            NadelResultInstruction.Set(
                subject = parentNode,
                key = NadelResultKey(overallField.resultKey),
                newValue = null,
            )
        } + NadelResultInstruction.AddError(transformContext.error)
    }
}
