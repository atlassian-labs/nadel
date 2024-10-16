package graphql.nadel.engine.transform.partition

import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.unwrapNonNull
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLList
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType

object NadelPartitionMutationPayloadMerger {

    fun mergeDataFromMutationPayloadLike(
        dataFromPartitionCalls: List<Any?>,
        thisNodesData: Any?,
        parentNodes: List<JsonNode>,
        overallField: ExecutableNormalizedField,
    ): List<NadelResultInstruction.Set> {
        val parentNode = parentNodes.first()

        // TODO: I'm not sure what's the best way to handle non-successful calls
        // - force `success` to be false?
        // - leave `success` alone?
        val nonSuccessPayload = mapOf("success" to false)

        val mutationPayloadLikeDataFromPartitionCalls = dataFromPartitionCalls
            .mapNotNull {
                if (it == null) {
                    nonSuccessPayload
                } else {
                    check(it is Map<*, *>) { "Expected a Map, but got ${it::class.simpleName}" }
                    it
                }
            }

        val thisNodesDataCast = thisNodesData?.let {
            check(it is Map<*, *>) { "Expected a Map, but got ${it::class.simpleName}" }
            it
        } ?: nonSuccessPayload

        val allListKeys = (thisNodesDataCast.keys + mutationPayloadLikeDataFromPartitionCalls.flatMap { it.keys })
            .distinct()

        val mergedData = mutationPayloadLikeDataFromPartitionCalls.fold(thisNodesDataCast) { acc, next ->
            allListKeys.associate {
                if (it == "success") {
                    Pair(it, acc[it].safeToBoolean() && next[it].safeToBoolean())
                } else {
                    if(acc[it] == null && next[it] == null) {
                        Pair(it, null)
                    } else {
                        Pair(it, acc[it].safeToList() + next[it].safeToList())
                    }
                }
            }
        }

        return listOf(
            NadelResultInstruction.Set(
                subject = parentNode,
                newValue = JsonNode(mergedData),
                field = overallField
            )
        )
    }

    /**
     * A GraphQL type is considered to be a "mutation payload" (for the purposes of this transform)  if
     * it has a `success` field of type `Boolean!` plus an `errors` field of type List and any other number
     * of fields of type List
     */
    fun GraphQLOutputType.isMutationPayloadLike(): Boolean {
        return this is GraphQLObjectType
            && this.getField("success")?.let { it.type.unwrapNonNull() as? GraphQLScalarType }?.name == "Boolean"
            && this.getField("errors") != null
            && this.fields.filter { it.name != "success" }.all { it.type.unwrapNonNull() is GraphQLList }
    }
}

fun Any?.safeToBoolean(): Boolean {
    return (this as? Boolean) ?: false
}

fun Any?.safeToList(): List<Any> {
    return (this as? List<Any>) ?: emptyList()
}
