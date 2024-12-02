package graphql.nadel.validation

import graphql.nadel.Service
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLSchema

data class NadelValidationContext internal constructor(
    val engineSchema: GraphQLSchema,
    val servicesByName: Map<String, Service>,
    val fieldContributor: Map<FieldCoordinates, Service>,
    val hydrationUnions: Set<String>,
    val namespaceTypeNames: Set<String>,
    val combinedTypeNames: Set<String>,
    val hiddenTypeNames: Set<String>,
    val instructionDefinitions: NadelInstructionDefinitionRegistry,
    val hook: NadelSchemaValidationHook,
) {
    internal val visitedTypes: MutableSet<NadelServiceSchemaElementRef> = hashSetOf()
}
