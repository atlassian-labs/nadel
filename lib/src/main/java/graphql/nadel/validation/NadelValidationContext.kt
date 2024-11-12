package graphql.nadel.validation

import graphql.nadel.Service
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLSchema

internal data class NadelValidationContext(
    val engineSchema: GraphQLSchema,
    val servicesByName: Map<String, Service>,
    val fieldContributor: Map<FieldCoordinates, Service>,
) {
    val visitedTypes: MutableSet<NadelServiceSchemaElementRef> = hashSetOf()
}
