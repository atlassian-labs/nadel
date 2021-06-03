package graphql.nadel.enginekt.transform

import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.artificial.ArtificialFields
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.util.JsonMap
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedField.newNormalizedField

object NadelTransformUtil {
    fun getOverallTypename(
        executionPlan: NadelExecutionPlan,
        artificialFields: ArtificialFields,
        node: JsonNode,
    ): String? {
        @Suppress("UNCHECKED_CAST")
        val nodeValueAsMap = node.value as? JsonMap ?: return null

        return if (artificialFields.typeNameResultKey in nodeValueAsMap) {
            executionPlan.getOverallTypeName(
                underlyingTypeName = nodeValueAsMap[artificialFields.typeNameResultKey] as String,
            )
        } else {
            null
        }
    }

    fun makeTypeNameField(
        artificialFields: ArtificialFields,
        objectTypeNames: List<String>,
    ): NormalizedField {
        return newNormalizedField()
            .alias(artificialFields.typeNameResultKey)
            .fieldName(TypeNameMetaFieldDef.name)
            .objectTypeNames(objectTypeNames)
            .build()
    }
}
