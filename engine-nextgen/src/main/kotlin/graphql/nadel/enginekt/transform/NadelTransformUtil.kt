package graphql.nadel.enginekt.transform

import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.util.JsonMap

object NadelTransformUtil {
    fun getOverallTypename(executionPlan: NadelExecutionPlan, node: JsonNode): String? {
        @Suppress("UNCHECKED_CAST")
        val nodeValueAsMap = node.value as? JsonMap ?: return null

        val key = TypeNameMetaFieldDef.name
        return if (key in nodeValueAsMap) {
            executionPlan.getOverallTypeName(
                underlyingTypeName = nodeValueAsMap[key] as String,
            )
        } else {
            null
        }
    }
}
