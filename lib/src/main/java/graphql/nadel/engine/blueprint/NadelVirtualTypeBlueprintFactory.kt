package graphql.nadel.engine.blueprint

import graphql.nadel.definition.hydration.getHydrationDefinitions
import graphql.nadel.definition.hydration.isHydrated
import graphql.nadel.engine.util.getFieldAt
import graphql.nadel.engine.util.unwrapAll
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema

internal class NadelVirtualTypeBlueprintFactory {
    private data class FactoryContext(
        val engineSchema: GraphQLSchema,
    )

    fun createVirtualTypeContexts(
        engineSchema: GraphQLSchema,
    ): List<NadelVirtualTypeContext> {
        val factoryContext = FactoryContext(
            engineSchema,
        )

        return with(factoryContext) {
            createVirtualTypeContexts()
        }
    }

    context(FactoryContext)
    private fun createVirtualTypeContexts(): List<NadelVirtualTypeContext> {
        return engineSchema
            .typeMap
            .values
            .asSequence()
            .filterIsInstance<GraphQLObjectType>()
            .flatMap { containerType ->
                containerType.fields
                    .asSequence()
                    .filter(GraphQLFieldDefinition::isHydrated)
                    .mapNotNull { virtualFieldDef ->
                        makeVirtualTypeContext(containerType, virtualFieldDef)
                    }
            }
            .toList()
    }

    fun makeVirtualTypeContext(
        engineSchema: GraphQLSchema,
        containerType: GraphQLObjectType,
        virtualFieldDef: GraphQLFieldDefinition,
    ): NadelVirtualTypeContext? {
        val factoryContext = FactoryContext(
            engineSchema,
        )

        return with(factoryContext) {
            makeVirtualTypeContext(containerType, virtualFieldDef)
        }
    }

    context(FactoryContext)
    private fun makeVirtualTypeContext(
        containerType: GraphQLObjectType,
        virtualFieldDef: GraphQLFieldDefinition,
    ): NadelVirtualTypeContext? {
        val typeMappings = createTypeMappings(virtualFieldDef)

        return if (typeMappings.isEmpty()) {
            return null
        } else {
            NadelVirtualTypeContext(
                virtualFieldContainer = containerType,
                virtualField = virtualFieldDef,
                virtualTypeToBackingType = typeMappings.virtualTypeToBackingTypeMap(),
                backingTypeToVirtualType = typeMappings.backingTypeToVirtualTypeMap(),
            )
        }
    }

    context(FactoryContext)
    private fun createTypeMappings(
        virtualFieldDef: GraphQLFieldDefinition,
    ): List<VirtualTypeMapping> {
        val hydration = virtualFieldDef.getHydrationDefinitions().first()
        val backingFieldDef = engineSchema.queryType.getFieldAt(hydration.backingField)!!
        val backingType = backingFieldDef.type.unwrapAll() as? GraphQLObjectType
            ?: return emptyList()
        val virtualType = virtualFieldDef.type.unwrapAll() as? GraphQLObjectType
            ?: return emptyList()

        // Not a virtual type
        if (virtualType.name == backingType.name) {
            return emptyList()
        }

        return createTypeMappings(
            backingType = backingType,
            virtualType = virtualType,
        )
    }

    context(FactoryContext)
    private fun createTypeMappings(
        backingType: GraphQLObjectType,
        virtualType: GraphQLObjectType,
    ): List<VirtualTypeMapping> {
        return listOf(VirtualTypeMapping(virtualType, backingType)) + createTypeMappings(
            virtualType = virtualType,
            backingType = backingType,
            visitedVirtualTypes = mutableSetOf(virtualType.name),
        )
    }

    context(FactoryContext)
    private fun createTypeMappings(
        virtualType: GraphQLObjectType,
        backingType: GraphQLObjectType,
        visitedVirtualTypes: MutableSet<String>,
    ): List<VirtualTypeMapping> {
        return virtualType
            .fields
            .flatMap { virtualFieldDef ->
                val virtualOutputType = virtualFieldDef.type.unwrapAll() as GraphQLObjectType
                if (visitedVirtualTypes.visit(virtualOutputType.name)) {
                    val backingFieldDef = backingType.getField(virtualFieldDef.name)
                    val backingOutputType = backingFieldDef.type.unwrapAll() as GraphQLObjectType

                    // Recursively create type mapping
                    val childTypeMappings = createTypeMappings(
                        visitedVirtualTypes = visitedVirtualTypes,
                        virtualType = virtualType,
                        backingType = backingType,
                    )

                    listOf(VirtualTypeMapping(virtualOutputType, backingOutputType)) + childTypeMappings
                } else {
                    emptyList()
                }
            }
    }

    data class VirtualTypeMapping(
        val virtualType: GraphQLObjectType,
        val backingType: GraphQLObjectType,
    )

    /**
     * Util to create [Map]
     */
    private fun List<VirtualTypeMapping>.virtualTypeToBackingTypeMap(): Map<String, String> {
        val mapping = mutableMapOf<String, String>()
        forEach {
            mapping[it.virtualType.name] = it.backingType.name
        }
        return mapping
    }

    /**
     * Util to create [Map]
     */
    private fun List<VirtualTypeMapping>.backingTypeToVirtualTypeMap(): Map<String, String> {
        val mapping = mutableMapOf<String, String>()
        forEach {
            mapping[it.backingType.name] = it.virtualType.name
        }
        return mapping
    }
}

/**
 * @return true if the element hasn't been visited before.
 * Also mutates the [MutableSet] to mark it as visited.
 */
private fun MutableSet<String>.visit(element: String): Boolean {
    return if (contains(element)) {
        false
    } else {
        add(element)
        true
    }
}
