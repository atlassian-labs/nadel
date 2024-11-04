package graphql.nadel.schema

import graphql.GraphQLException
import graphql.language.FieldDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.ObjectTypeDefinition.newObjectTypeDefinition
import graphql.language.ScalarTypeDefinition.newScalarTypeDefinition
import graphql.language.SchemaDefinition
import graphql.language.SourceLocation
import graphql.nadel.NadelDefinitionRegistry
import graphql.nadel.NadelOperationKind
import graphql.nadel.util.AnyNamedNode
import graphql.nadel.util.AnySDLDefinition
import graphql.nadel.util.AnySDLNamedDefinition
import graphql.nadel.util.isExtensionDef
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.WiringFactory

internal class OverallSchemaGenerator {
    fun buildOverallSchema(
        serviceRegistries: List<NadelDefinitionRegistry>,
        wiringFactory: WiringFactory,
    ): GraphQLSchema {
        val schemaGenerator = SchemaGenerator()
        val runtimeWiring = RuntimeWiring.newRuntimeWiring()
            .wiringFactory(wiringFactory)
            .build()

        return schemaGenerator.makeExecutableSchema(createTypeRegistry(serviceRegistries), runtimeWiring)
    }

    private fun createTypeRegistry(serviceRegistries: List<NadelDefinitionRegistry>): TypeDefinitionRegistry {
        val topLevelFields = NadelOperationKind.entries
            .associateWith {
                mutableListOf<FieldDefinition>()
            }

        val overallRegistry = TypeDefinitionRegistry()
        val allDefinitions = ArrayList<AnySDLDefinition>()

        for (definitionRegistry in serviceRegistries) {
            collectTypes(topLevelFields, allDefinitions, definitionRegistry)
        }

        // Create merged operation types in schema for all top level fields
        topLevelFields.keys.forEach { operationKind ->
            val fields = topLevelFields[operationKind]
            if (fields?.isNotEmpty() == true) {
                overallRegistry.add(
                    newObjectTypeDefinition()
                        .name(operationKind.defaultTypeName)
                        .sourceLocation(SourceLocation(-1, -1, "Generated"))
                        .fieldDefinitions(fields)
                        .build()
                )
            }
        }

        // add our custom directives if they are not present
        addIfNotPresent(overallRegistry, allDefinitions, NadelDirectives.nadelHydrationArgumentDefinition)
        addIfNotPresent(overallRegistry, allDefinitions, NadelDirectives.hydratedDirectiveDefinition)
        addIfNotPresent(overallRegistry, allDefinitions, NadelDirectives.renamedDirectiveDefinition)
        addIfNotPresent(overallRegistry, allDefinitions, NadelDirectives.hiddenDirectiveDefinition)
        addIfNotPresent(overallRegistry, allDefinitions, NadelDirectives.nadelBatchObjectIdentifiedByDefinition)
        addIfNotPresent(overallRegistry, allDefinitions, NadelDirectives.nadelHydrationResultFieldPredicateDefinition)
        addIfNotPresent(overallRegistry, allDefinitions, NadelDirectives.nadelHydrationResultConditionDefinition)
        addIfNotPresent(overallRegistry, allDefinitions, NadelDirectives.nadelHydrationConditionDefinition)
        addIfNotPresent(overallRegistry, allDefinitions, NadelDirectives.deferDirectiveDefinition)
        addIfNotPresent(overallRegistry, allDefinitions, NadelDirectives.namespacedDirectiveDefinition)
        addIfNotPresent(overallRegistry, allDefinitions, NadelDirectives.partitionDirectiveDefinition)

        addIfNotPresent(
            overallRegistry, allDefinitions, newScalarTypeDefinition()
                .name(ExtendedScalars.Json.name)
                .build()
        )

        for (definition in allDefinitions) {
            val error = overallRegistry.add(definition)
            if (error.isPresent) {
                throw GraphQLException("Unable to add definition to overall registry: " + error.get().message)
            }
        }

        return overallRegistry
    }

    private inline fun <reified T : AnySDLNamedDefinition> addIfNotPresent(
        overallRegistry: TypeDefinitionRegistry,
        allDefinitions: List<AnySDLDefinition>,
        namedDefinition: T,
    ) {
        if (!containsElement(allDefinitions, namedDefinition)) {
            overallRegistry.add(namedDefinition)
        }
    }

    private inline fun <reified T : AnySDLNamedDefinition> containsElement(
        allDefinitions: List<AnySDLDefinition>,
        def: T,
    ): Boolean {
        return allDefinitions
            .asSequence()
            .filterIsInstance<AnyNamedNode>()
            .filter { it.name == def.name }
            // if it's an `extent type Foo` then it does not count since we need an actual `type Foo` defined
            .filterNot { it.isExtensionDef }
            .any { element ->
                require(element is T) {
                    val name = def.name
                    val expected = T::class.java.name
                    val actual = element.javaClass.name
                    "The element schema $name is expected to be a $expected but is in fact a $actual"
                }

                element.name == def.name
            }
    }

    private fun collectTypes(
        topLevelFields: Map<NadelOperationKind, MutableList<FieldDefinition>>,
        allDefinitions: MutableList<AnySDLDefinition>,
        definitionRegistry: NadelDefinitionRegistry,
    ) {
        val opDefinitions = definitionRegistry.operationMap
        val opTypeNames: MutableSet<String> = HashSet(3)

        opDefinitions.keys.forEach { opType: NadelOperationKind ->
            val opsDefinitions = opDefinitions[opType]
            if (opsDefinitions != null) {
                // Collect field definitions
                for (objectTypeDefinition in opsDefinitions) {
                    topLevelFields[opType]!!.addAll(objectTypeDefinition.fieldDefinitions)
                }

                // Record down the type name for each operation
                val operationTypeName = definitionRegistry.getOperationTypeName(opType)
                opTypeNames.add(operationTypeName)
            }
        }

        definitionRegistry
            .definitions
            .asSequence()
            .filter { definition ->
                if (definition is ObjectTypeDefinition) {
                    // Don't add operation types
                    !opTypeNames.contains(definition.name)
                } else {
                    true
                }
            }
            .filter { definition ->
                // Don't add schema definitions, everything gets merged into a generated type with the default name
                definition !is SchemaDefinition
            }
            .forEach(allDefinitions::add)
    }
}
