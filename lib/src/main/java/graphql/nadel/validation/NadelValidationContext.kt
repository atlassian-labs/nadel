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
    private val visitedTypes: MutableSet<NadelServiceSchemaElementRef> = hashSetOf()

    /**
     * @return true to visit
     */
    fun visitElement(schemaElement: NadelServiceSchemaElement): Boolean {
        // Returns true if the element was added i.e. haven't visited before
        return visitedTypes.add(schemaElement.toRef())
    }
}
