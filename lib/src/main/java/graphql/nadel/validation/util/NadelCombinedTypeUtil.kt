package graphql.nadel.validation.util

import graphql.nadel.Service
import graphql.nadel.engine.util.AnyImplementingTypeDefinition
import graphql.nadel.validation.NadelServiceSchemaElement
import graphql.nadel.validation.util.NadelCombinedTypeUtil.getFieldsThatServiceContributed
import graphql.schema.GraphQLNamedSchemaElement

object NadelCombinedTypeUtil {
    fun getFieldsThatServiceContributed(schemaElement: NadelServiceSchemaElement): Set<String> {
        return getFieldsThatServiceContributed(
            service = schemaElement.service,
            overallType = schemaElement.overall,
        )
    }

    /**
     * For a combined type (e.g. operation types) get the field that this [service] actually
     * contributed to the type.
     *
     * e.g.
     *
     * ```graphql
     * # service A
     * type Query {
     *   echo: String
     * }
     *
     * # service B
     * type Query {
     *   time: DateTime
     * }
     * ```
     *
     * If we ask [getFieldsThatServiceContributed] for service A for combined
     * type `Query` then the result is `[echo]`.
     *
     * If we ask [getFieldsThatServiceContributed] for service B for combined
     * type `Query` then the result is `[time]`.
     */
    fun getFieldsThatServiceContributed(service: Service, overallType: GraphQLNamedSchemaElement): Set<String> {
        return service.definitionRegistry.getDefinitions(overallType.name)
            .asSequence()
            .filterIsInstance<AnyImplementingTypeDefinition>()
            .flatMap { it.fieldDefinitions }
            .map { it.name }
            .toSet()
    }
}
