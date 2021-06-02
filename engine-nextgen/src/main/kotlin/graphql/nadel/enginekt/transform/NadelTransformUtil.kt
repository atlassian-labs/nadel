package graphql.nadel.enginekt.transform

import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.util.JsonMap

object NadelTransformUtil {
    fun getOverallTypename(
        executionPlan: NadelExecutionPlan,
        node: JsonNode,
        typeNameResultKey: String = TypeNameMetaFieldDef.name,
    ): String? {
        @Suppress("UNCHECKED_CAST")
        val nodeValueAsMap = node.value as? JsonMap ?: return null

        return if (typeNameResultKey in nodeValueAsMap) {
            executionPlan.getOverallTypeName(
                underlyingTypeName = nodeValueAsMap[typeNameResultKey] as String,
            )
        } else {
            null
        }
    }
}
