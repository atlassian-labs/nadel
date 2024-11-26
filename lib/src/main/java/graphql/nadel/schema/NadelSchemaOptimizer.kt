package graphql.nadel.schema

import graphql.introspection.Introspection
import graphql.language.Description
import graphql.language.EnumTypeDefinition
import graphql.language.EnumTypeExtensionDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InputObjectTypeExtensionDefinition
import graphql.language.InterfaceTypeDefinition
import graphql.language.InterfaceTypeExtensionDefinition
import graphql.language.NodeBuilder
import graphql.language.ObjectTypeDefinition
import graphql.language.ObjectTypeExtensionDefinition
import graphql.language.ScalarTypeDefinition
import graphql.language.ScalarTypeExtensionDefinition
import graphql.language.SourceLocation
import graphql.language.UnionTypeDefinition
import graphql.language.UnionTypeExtensionDefinition
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLCodeRegistry
import graphql.schema.idl.TypeDefinitionRegistry

/**
 * Optimizes schema by deleting a bunch of unused things e.g.
 *
 * 1. Descriptions from underlying schema
 * 2. Unused values in [GraphQLCodeRegistry]
 *
 * etc.
 */
internal object NadelSchemaOptimizer {
    private val noDescription = Description("", SourceLocation.EMPTY, false)

    /**
     * [GraphQLCodeRegistry] is useless for us, but it can be quite large if the schema
     * has lots of fields. So we should delete it.
     */
    fun cleanCodeRegistry(codeRegistry: GraphQLCodeRegistry) {
        try {
            val dataFetcherMap = codeRegistry.javaClass.getDeclaredField("dataFetcherMap")
                ?: return
            dataFetcherMap.trySetAccessible()

            val map = dataFetcherMap.get(codeRegistry) as MutableMap<*, *>
            try {
                map.keys.toList()
                    .forEach { key ->
                        if (key is FieldCoordinates) {
                            if (!Introspection.isIntrospectionTypes(key.typeName)) {
                                map.remove(key)
                            }
                        }
                    }
            } catch (ignored: UnsupportedOperationException) {
            }
        } catch (ignored: ReflectiveOperationException) {
        }
    }

    fun deleteUselessUnderlyingSchemaElements(
        typeDefinitions: TypeDefinitionRegistry,
        captureSourceLocation: Boolean,
    ): TypeDefinitionRegistry {
        fun <T : NodeBuilder> T.dropSourceLocations(): T {
            if (captureSourceLocation) {
                return this
            }
            sourceLocation(SourceLocation.EMPTY)
            return this
        }

        val newDefinitions = typeDefinitions.parseOrder.inOrder.values
            .asSequence()
            .flatten()
            .map { definition ->
                when (definition) {
                    is EnumTypeExtensionDefinition -> {
                        definition.transformExtension { builder ->
                            builder
                                .dropSourceLocations()
                                .description(noDescription)
                                .comments(emptyList())
                        }
                    }
                    is InputObjectTypeExtensionDefinition -> {
                        definition.transformExtension { builder ->
                            builder
                                .dropSourceLocations()
                                .description(noDescription)
                                .comments(emptyList())
                        }
                    }
                    is InterfaceTypeExtensionDefinition -> {
                        definition.transformExtension { builder ->
                            builder
                                .dropSourceLocations()
                                .description(noDescription)
                                .comments(emptyList())
                        }
                    }
                    is ObjectTypeExtensionDefinition -> {
                        definition.transformExtension { builder ->
                            builder
                                .dropSourceLocations()
                                .description(noDescription)
                                .comments(emptyList())
                        }
                    }
                    is ScalarTypeExtensionDefinition -> {
                        definition.transformExtension { builder ->
                            builder
                                .dropSourceLocations()
                                .description(noDescription)
                                .comments(emptyList())
                        }
                    }
                    is UnionTypeExtensionDefinition -> {
                        definition.transformExtension { builder ->
                            builder
                                .dropSourceLocations()
                                .description(noDescription)
                                .comments(emptyList())
                        }
                    }
                    is EnumTypeDefinition -> {
                        definition.transform { builder ->
                            builder
                                .dropSourceLocations()
                                .description(noDescription)
                                .comments(emptyList())
                        }
                    }
                    is InputObjectTypeDefinition -> {
                        definition.transform { builder ->
                            builder
                                .dropSourceLocations()
                                .description(noDescription)
                                .comments(emptyList())
                        }
                    }
                    is InterfaceTypeDefinition -> {
                        definition.transform { builder ->
                            builder
                                .dropSourceLocations()
                                .description(noDescription)
                                .comments(emptyList())
                        }
                    }
                    is ObjectTypeDefinition -> {
                        definition.transform { builder ->
                            builder
                                .dropSourceLocations()
                                .description(noDescription)
                                .comments(emptyList())
                        }
                    }
                    is ScalarTypeDefinition -> {
                        definition.transform { builder ->
                            builder
                                .dropSourceLocations()
                                .description(noDescription)
                                .comments(emptyList())
                        }
                    }
                    is UnionTypeDefinition -> {
                        definition.transform { builder ->
                            builder
                                .dropSourceLocations()
                                .description(noDescription)
                                .comments(emptyList())
                        }
                    }
                    else -> {
                        definition
                    }
                }
            }
            .toList()

        return TypeDefinitionRegistry()
            .also { newRegistry ->
                newRegistry.addAll(newDefinitions)
            }
    }
}
