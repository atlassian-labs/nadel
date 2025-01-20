package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.definition.NadelDefinition
import graphql.nadel.definition.NadelSchemaMemberCoordinates
import graphql.nadel.definition.coordinates
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.definition.partition.NadelPartitionDefinition
import graphql.nadel.definition.renamed.NadelRenamedDefinition
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLSchema

data class NadelValidationContext internal constructor(
    val engineSchema: GraphQLSchema,
    val servicesByName: Map<String, Service>,
    val fieldContributor: Map<FieldCoordinates, Service>,
    val hydrationUnions: Set<String>,
    val namespaceTypeNames: Set<String>,
    val combinedTypeNames: Set<String>,
    val hiddenTypeNames: Set<String>,
    val definitions: Map<NadelSchemaMemberCoordinates, List<NadelDefinition>>,
    val hook: NadelSchemaValidationHook,
) {
    internal val visitedTypes: MutableSet<NadelServiceSchemaElementRef> = hashSetOf()
}

context(NadelValidationContext)
fun isHydrated(
    container: GraphQLFieldsContainer,
    field: GraphQLFieldDefinition,
): Boolean {
    return getHydrationDefinitions(container, field).any()
}

context(NadelValidationContext)
fun isHydrated(
    container: NadelServiceSchemaElement.FieldsContainer,
    field: GraphQLFieldDefinition,
): Boolean {
    return getHydrationDefinitions(coords(container, field)).any()
}

context(NadelValidationContext)
fun getHydrationDefinitions(
    container: NadelServiceSchemaElement.FieldsContainer,
    field: GraphQLFieldDefinition,
): Sequence<NadelHydrationDefinition> {
    return getHydrationDefinitions(coords(container, field))
}

context(NadelValidationContext)
fun getHydrationDefinitions(
    container: GraphQLFieldsContainer,
    field: GraphQLFieldDefinition,
): Sequence<NadelHydrationDefinition> {
    return getHydrationDefinitions(container.coordinates().field(field.name))
}

context(NadelValidationContext)
fun getHydrationDefinitions(
    coords: NadelSchemaMemberCoordinates.Field,
): Sequence<NadelHydrationDefinition> {
    val definitions = definitions[coords]
        ?: return emptySequence()

    return definitions.asSequence()
        .filterIsInstance<NadelHydrationDefinition>()
}

context(NadelValidationContext)
fun isRenamed(
    container: NadelServiceSchemaElement.Type,
): Boolean {
    return getRenamedOrNull(container.overall) != null
}

context(NadelValidationContext)
fun isRenamed(
    container: GraphQLFieldsContainer,
    field: GraphQLFieldDefinition,
): Boolean {
    return getRenamedOrNull(container, field) != null
}

context(NadelValidationContext)
fun isRenamed(
    container: NadelServiceSchemaElement.FieldsContainer,
    field: GraphQLFieldDefinition,
): Boolean {
    return getRenamedOrNull(coords(container, field)) != null
}

context(NadelValidationContext)
fun getRenamedOrNull(
    container: GraphQLFieldsContainer,
    field: GraphQLFieldDefinition,
): NadelRenamedDefinition.Field? {
    val coords = container.coordinates().field(field.name)
    return getRenamedOrNull(coords)
}

context(NadelValidationContext)
fun getRenamedOrNull(
    container: NadelServiceSchemaElement.FieldsContainer,
    field: GraphQLFieldDefinition,
): NadelRenamedDefinition.Field? {
    return getRenamedOrNull(coords(container, field))
}

context(NadelValidationContext)
fun getRenamedOrNull(coords: NadelSchemaMemberCoordinates.Field): NadelRenamedDefinition.Field? {
    val definitions = definitions[coords]
        ?: return null

    return definitions.asSequence()
        .filterIsInstance<NadelRenamedDefinition.Field>()
        .firstOrNull()
}

context(NadelValidationContext)
fun getRenamedOrNull(
    container: GraphQLNamedType,
): NadelRenamedDefinition.Type? {
    return getRenamedOrNull(container.coordinates())
}

context(NadelValidationContext)
fun getRenamedOrNull(coords: NadelSchemaMemberCoordinates.Type): NadelRenamedDefinition.Type? {
    val definitions = definitions[coords]
        ?: return null

    return definitions.asSequence()
        .filterIsInstance<NadelRenamedDefinition.Type>()
        .firstOrNull()
}

context(NadelValidationContext)
fun getUnderlyingTypeName(type: GraphQLNamedType): String {
    return getRenamedOrNull(type)?.from ?: type.name
}

context(NadelValidationContext)
fun isPartitioned(
    container: GraphQLFieldsContainer,
    field: GraphQLFieldDefinition,
): Boolean {
    return getPartitionedOrNull(container, field) != null
}

context(NadelValidationContext)
fun isPartitioned(
    container: NadelServiceSchemaElement.FieldsContainer,
    field: GraphQLFieldDefinition,
): Boolean {
    return getPartitionedOrNull(coords(container, field)) != null
}

context(NadelValidationContext)
fun getPartitionedOrNull(
    container: NadelServiceSchemaElement.FieldsContainer,
    field: GraphQLFieldDefinition,
): NadelPartitionDefinition? {
    return getPartitionedOrNull(coords(container, field))
}

context(NadelValidationContext)
fun getPartitionedOrNull(
    container: GraphQLFieldsContainer,
    field: GraphQLFieldDefinition,
): NadelPartitionDefinition? {
    val coords = container.coordinates().field(field.name)
    return getPartitionedOrNull(coords)
}

context(NadelValidationContext)
fun getPartitionedOrNull(
    coords: NadelSchemaMemberCoordinates.Field,
): NadelPartitionDefinition? {
    val definitions = definitions[coords]
        ?: return null

    return definitions.asSequence()
        .filterIsInstance<NadelPartitionDefinition>()
        .firstOrNull()
}

private fun coords(
    container: NadelServiceSchemaElement.FieldsContainer,
    field: GraphQLFieldDefinition,
): NadelSchemaMemberCoordinates.Field {
    val containerCoords = when (container) {
        is NadelServiceSchemaElement.Interface -> NadelSchemaMemberCoordinates.Interface(container.overall.name)
        is NadelServiceSchemaElement.Object -> NadelSchemaMemberCoordinates.Object(container.overall.name)
    }

    return containerCoords.field(field.name)
}
