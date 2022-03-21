package graphql.nadel.validation.util

import graphql.nadel.Service
import graphql.nadel.enginekt.util.AnyImplementingTypeDefinition
import graphql.nadel.enginekt.util.unwrapAll
import graphql.nadel.schema.NadelDirectives
import graphql.nadel.validation.NadelServiceSchemaElement
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLSchema

object NadelCombinedTypeUtil {
    fun getFieldsThatServiceContributed(schemaElement: NadelServiceSchemaElement): Set<String> {
        return getFieldsThatServiceContributed(
            service = schemaElement.service,
            overallType = schemaElement.overall,
        )
    }

    /**
     * For a combined type as defined by [isCombinedType] get the fields
     * that given [service] actually contributed to the type.
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
        return service.definitionRegistry.definitions
            .asSequence()
            .filterIsInstance<AnyImplementingTypeDefinition>()
            .filter { it.name == overallType.name }
            .flatMap { it.fieldDefinitions }
            .map { it.name }
            .toSet()
    }

    /**
     * Determines whether a type has fields backed by different services.
     *
     * e.g.
     *
     * ```graphql
     * type Query {
     *   echo: String # Backed by testing service
     *   user(id: ID): User # Backed by identity service
     * }
     * ```
     *
     * That is, validation for these types must only occur for the fields on
     * the type must be done against multiple underlying types.
     */
    fun isCombinedType(overallSchema: GraphQLSchema, type: GraphQLNamedSchemaElement): Boolean {
        val usesTypeAsNamespaced = { field: GraphQLFieldDefinition ->
            field.hasDirective(NadelDirectives.namespacedDirectiveDefinition.name)
                && field.type.unwrapAll().name == type.name
        }

        // IF YOU CHANGE THIS then check NadelTypeValidation.getTypeNamesUsed to ensure they type is actually visited
        return type.name == overallSchema.queryType.name
            || type.name == overallSchema.mutationType?.name
            || type.name == overallSchema.subscriptionType?.name
            || overallSchema.queryType.fields.any(usesTypeAsNamespaced)
            || overallSchema.mutationType?.fields?.any(usesTypeAsNamespaced) == true
            || overallSchema.subscriptionType?.fields?.any(usesTypeAsNamespaced) == true
    }
}
