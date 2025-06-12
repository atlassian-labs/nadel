package graphql.nadel.validation

import graphql.nadel.definition.NadelInstructionDefinition
import graphql.nadel.definition.coordinates.NadelFieldCoordinates
import graphql.nadel.definition.coordinates.NadelInterfaceCoordinates
import graphql.nadel.definition.coordinates.NadelObjectCoordinates
import graphql.nadel.definition.coordinates.NadelSchemaMemberCoordinates
import graphql.nadel.definition.coordinates.NadelTypeCoordinates
import graphql.nadel.definition.coordinates.coordinates
import graphql.nadel.definition.hydration.NadelDefaultHydrationDefinition
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.definition.partition.NadelPartitionDefinition
import graphql.nadel.definition.renamed.NadelRenamedDefinition
import graphql.nadel.definition.stubbed.NadelStubbedDefinition
import graphql.nadel.engine.util.emptyOrSingle
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType
import kotlin.reflect.KClass

data class NadelInstructionDefinitionRegistry(
    private val definitions: Map<NadelSchemaMemberCoordinates, List<NadelInstructionDefinition>>,
) {
    fun isHydrated(
        container: GraphQLFieldsContainer,
        field: GraphQLFieldDefinition,
    ): Boolean {
        return getHydrationDefinitions(container, field).any()
    }

    fun isHydrated(
        container: NadelServiceSchemaElement.FieldsContainer,
        field: GraphQLFieldDefinition,
    ): Boolean {
        return getHydrationDefinitionSequence(coords(container, field)).any()
    }

    fun getHydrationDefinitions(
        container: NadelServiceSchemaElement.FieldsContainer,
        field: GraphQLFieldDefinition,
    ): Sequence<NadelHydrationDefinition> {
        return getHydrationDefinitionSequence(coords(container, field))
    }

    fun getHydrationDefinitions(
        container: GraphQLFieldsContainer,
        field: GraphQLFieldDefinition,
    ): Sequence<NadelHydrationDefinition> {
        return getHydrationDefinitionSequence(container.coordinates().field(field.name))
    }

    fun getHydrationDefinitionSequence(
        coords: NadelFieldCoordinates,
    ): Sequence<NadelHydrationDefinition> {
        val definitions = definitions[coords]
            ?: return emptySequence()

        return definitions.asSequence()
            .filterIsInstance<NadelHydrationDefinition>()
    }

    fun isRenamed(
        container: NadelServiceSchemaElement.Type,
    ): Boolean {
        return getRenamedOrNull(container.overall) != null
    }

    fun isRenamed(
        container: GraphQLFieldsContainer,
        field: GraphQLFieldDefinition,
    ): Boolean {
        return getRenamedOrNull(container, field) != null
    }

    fun isRenamed(
        container: NadelServiceSchemaElement.FieldsContainer,
        field: GraphQLFieldDefinition,
    ): Boolean {
        return getRenamedOrNull(coords(container, field)) != null
    }

    fun isStubbed(
        container: NadelServiceSchemaElement.FieldsContainer,
        field: GraphQLFieldDefinition,
    ): Boolean {
        return getStubbedOrNull(coords(container, field)) != null
    }

    fun isStubbed(
        container: GraphQLFieldsContainer,
        field: GraphQLFieldDefinition,
    ): Boolean {
        return getStubbedOrNull(container.coordinates().field(field.name)) != null
    }

    fun isStubbed(container: NadelServiceSchemaElement.Object): Boolean {
        return isStubbed(container.overall)
    }

    fun isStubbed(container: GraphQLObjectType): Boolean {
        return getStubbedOrNull(container.coordinates()) != null
    }

    fun getRenamedOrNull(
        container: GraphQLFieldsContainer,
        field: GraphQLFieldDefinition,
    ): NadelRenamedDefinition.Field? {
        val coords = container.coordinates().field(field.name)
        return getRenamedOrNull(coords)
    }

    fun getRenamedOrNull(
        container: NadelServiceSchemaElement.FieldsContainer,
        field: GraphQLFieldDefinition,
    ): NadelRenamedDefinition.Field? {
        return getRenamedOrNull(coords(container, field))
    }

    fun getRenamedOrNull(coords: NadelFieldCoordinates): NadelRenamedDefinition.Field? {
        val definitions = definitions[coords]
            ?: return null

        return definitions.asSequence()
            .filterIsInstance<NadelRenamedDefinition.Field>()
            .firstOrNull()
    }

    fun getStubbedOrNull(coords: NadelFieldCoordinates): NadelStubbedDefinition? {
        val definitions = definitions[coords]
            ?: return null

        return definitions.asSequence()
            .filterIsInstance<NadelStubbedDefinition>()
            .firstOrNull()
    }

    fun getStubbedOrNull(coords: NadelObjectCoordinates): NadelStubbedDefinition? {
        val definitions = definitions[coords]
            ?: return null

        return definitions.asSequence()
            .filterIsInstance<NadelStubbedDefinition>()
            .firstOrNull()
    }

    fun getRenamedOrNull(
        container: GraphQLNamedType,
    ): NadelRenamedDefinition.Type? {
        return getRenamedOrNull(container.coordinates())
    }

    fun getRenamedOrNull(coords: NadelTypeCoordinates): NadelRenamedDefinition.Type? {
        val definitions = definitions[coords]
            ?: return null

        return definitions.asSequence()
            .filterIsInstance<NadelRenamedDefinition.Type>()
            .firstOrNull()
    }

    fun getUnderlyingTypeName(type: GraphQLNamedType): String {
        return getRenamedOrNull(type)?.from ?: type.name
    }

    fun isPartitioned(
        container: GraphQLFieldsContainer,
        field: GraphQLFieldDefinition,
    ): Boolean {
        return getPartitionedOrNull(container, field) != null
    }

    fun isPartitioned(
        container: NadelServiceSchemaElement.FieldsContainer,
        field: GraphQLFieldDefinition,
    ): Boolean {
        return getPartitionedOrNull(coords(container, field)) != null
    }

    inline fun <reified T : NadelInstructionDefinition> hasInstructionsOtherThan(
        container: NadelServiceSchemaElement.FieldsContainer,
        field: GraphQLFieldDefinition,
    ): Boolean {
        return hasInstructionsOtherThan(T::class, container, field)
    }

    fun <T : NadelInstructionDefinition> hasInstructionsOtherThan(
        instructionType: KClass<T>,
        container: NadelServiceSchemaElement.FieldsContainer,
        field: GraphQLFieldDefinition,
    ): Boolean {
        return hasInstructionsOtherThan(instructionType, coords(container, field))
    }

    fun getPartitionedOrNull(
        container: NadelServiceSchemaElement.FieldsContainer,
        field: GraphQLFieldDefinition,
    ): NadelPartitionDefinition? {
        return getPartitionedOrNull(coords(container, field))
    }

    fun getPartitionedOrNull(
        container: GraphQLFieldsContainer,
        field: GraphQLFieldDefinition,
    ): NadelPartitionDefinition? {
        val coords = container.coordinates().field(field.name)
        return getPartitionedOrNull(coords)
    }

    fun getPartitionedOrNull(
        coords: NadelFieldCoordinates,
    ): NadelPartitionDefinition? {
        val definitions = definitions[coords]
            ?: return null

        return definitions.asSequence()
            .filterIsInstance<NadelPartitionDefinition>()
            .firstOrNull()
    }

    inline fun <reified T : NadelInstructionDefinition> hasInstructionsOtherThan(
        coords: NadelFieldCoordinates,
    ): Boolean {
        return hasInstructionsOtherThan(T::class, coords)
    }

    fun <T : NadelInstructionDefinition> hasInstructionsOtherThan(
        instructionType: KClass<T>,
        coords: NadelFieldCoordinates,
    ): Boolean {
        val instructions = definitions[coords] ?: return false
        return instructions
            .asSequence()
            .filterNot {
                instructionType.isInstance(it)
            }
            .any()
    }

    fun hasDefaultHydration(
        type: NadelServiceSchemaElement.Type,
    ): Boolean {
        return getDefaultHydrationSequence(type.overall.coordinates()).any()
    }

    fun getDefaultHydrationOrNull(
        type: NadelServiceSchemaElement.Type,
    ): NadelDefaultHydrationDefinition? {
        return getDefaultHydrationSequence(type.overall.coordinates()).emptyOrSingle()
    }

    private fun getDefaultHydrationSequence(
        coords: NadelTypeCoordinates,
    ): Sequence<NadelDefaultHydrationDefinition> {
        val definitions = definitions[coords]
            ?: return emptySequence()

        return definitions
            .asSequence()
            .filterIsInstance<NadelDefaultHydrationDefinition>()
    }

    private fun coords(
        container: NadelServiceSchemaElement.FieldsContainer,
        field: GraphQLFieldDefinition,
    ): NadelFieldCoordinates {
        val containerCoords = when (container) {
            is NadelServiceSchemaElement.Interface -> NadelInterfaceCoordinates(container.overall.name)
            is NadelServiceSchemaElement.Object -> NadelObjectCoordinates(container.overall.name)
        }

        return containerCoords.field(field.name)
    }
}
