package graphql.nadel.engine.transform

import graphql.introspection.Introspection
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelVirtualTypeContext
import graphql.nadel.engine.transform.NadelTypeRenameResultTransform.TransformFieldContext
import graphql.nadel.engine.transform.NadelTypeRenameResultTransform.TransformOperationContext
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.queryPath
import graphql.normalized.ExecutableNormalizedField

internal class NadelTypeRenameResultTransform : NadelTransform<TransformOperationContext, TransformFieldContext> {
    data class TransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
    ) : NadelTransformOperationContext()

    data class TransformFieldContext(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
        val typeRenamePath: NadelQueryPath,
        val virtualTypeContext: NadelVirtualTypeContext?,
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
        return if (overallField.fieldName == Introspection.TypeNameMetaFieldDef.name) {
            val virtualTypeContext =
                (transformContext.operationExecutionContext.hydrationDetails?.instruction as? NadelHydrationFieldInstruction)?.virtualTypeContext

            TransformFieldContext(
                parentContext = transformContext,
                overallField = overallField,
                typeRenamePath = overallField.queryPath,
                virtualTypeContext = virtualTypeContext,
            )
        } else {
            null
        }
    }

    override suspend fun transformField(
        transformContext: TransformFieldContext,
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult.unmodified(field)
    }

    override suspend fun transformResult(
        transformContext: TransformFieldContext,
        underlyingParentField: ExecutableNormalizedField?,
        resultNodes: JsonNodes,
    ): List<NadelResultInstruction> {
        val service = transformContext.service
        val executionBlueprint = transformContext.executionBlueprint
        val overallField = transformContext.overallField
        val parentNodes = resultNodes.getNodesAt(
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
            ).let { overallTypeName ->
                // Try to map it to a virtual typename
                transformContext.virtualTypeContext?.backingTypeToVirtualType?.get(overallTypeName)
                    ?: overallTypeName
            }

            val typeName: String = if (transformContext.executionContext.hints.sharedTypeRenames(service)) {
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
