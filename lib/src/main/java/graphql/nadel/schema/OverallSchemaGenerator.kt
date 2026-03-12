package graphql.nadel.schema

import graphql.GraphQLException
import graphql.language.EnumValue
import graphql.language.FieldDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.ObjectTypeDefinition.newObjectTypeDefinition
import graphql.language.ScalarTypeDefinition.newScalarTypeDefinition
import graphql.language.SchemaDefinition
import graphql.language.SourceLocation
import graphql.nadel.NadelOperationKind
import graphql.nadel.NadelTypeDefinitionRegistry
import graphql.nadel.util.AnyNamedNode
import graphql.nadel.util.AnySDLDefinition
import graphql.nadel.util.AnySDLNamedDefinition
import graphql.nadel.util.isExtensionDef
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaPrinter
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.WiringFactory
import graphql.schema.transform.FieldVisibilitySchemaTransformation
import graphql.schema.transform.VisibleFieldPredicate
import graphql.schema.transform.VisibleFieldPredicateEnvironment
import java.io.File
import kotlin.time.measureTimedValue

object HideFieldsInEnvironmentTypeSupport {
    fun apply(originalSchema: GraphQLSchema): GraphQLSchema {
        return FieldVisibilitySchemaTransformation(HideFieldInEnvironmentType()).apply(originalSchema)
    }
}

class HideFieldInEnvironmentType : VisibleFieldPredicate {
    private val stageToHide: String = "STAGING"

    /**
     * A field is considered as not visible when it has the @lifecycle directive
     * with the `stage` argument equals to LifecycleStage passed in.
     */
    override fun isVisible(environment: VisibleFieldPredicateEnvironment): Boolean {
        if (environment.schemaElement !is GraphQLDirectiveContainer) {
            return true
        }

        val directiveContainer = (environment.schemaElement as GraphQLDirectiveContainer)
        val lifecycleDirective = directiveContainer.getAppliedDirective("lifecycle") ?: return true
        val stageArgument = lifecycleDirective.getArgument("stage") ?: return true
        return stageArgument.getValue<String>() != stageToHide
    }
}

internal class OverallSchemaGenerator {
    fun buildOverallSchema(
        serviceRegistries: List<NadelTypeDefinitionRegistry>,
        wiringFactory: WiringFactory,
        schemaDefinitionTransformationHook: NadelSchemaDefinitionTransformationHook,
    ): GraphQLSchema {
        val schemaGenerator = SchemaGenerator()
        val runtimeWiring = RuntimeWiring.newRuntimeWiring()
            .wiringFactory(wiringFactory)
            .codeRegistry(NeverWiringFactory.NEVER_CODE_REGISTRY)
            .build()

        val definitions = getDefinitions(serviceRegistries)
            .let {
                schemaDefinitionTransformationHook.apply(it)
            }
            .also {
                val (newDefs, newDefsTime) = measureTimedValue {
                    NadelFieldDefinitionVisibilityTransformation { parent, field ->
                        val lifecycle = field.getDirectives("lifecycle").singleOrNull()
                        if (lifecycle == null) {
                            true
                        } else {
                            (lifecycle.getArgument("stage").value as EnumValue).name != "STAGING"
                        }
                    }.apply(it)
                }
                println("Took ${newDefsTime.inWholeMilliseconds}ms to transform DEFINITIONS to hide lifecycle fields")

                val typeRegistry = createTypeRegistry(newDefs)
                File("hidden-faster.graphqls").writeText(
                    SchemaPrinter().print(
                        schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring)
                    )
                )
            }
        val typeRegistry = createTypeRegistry(definitions)

        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring)
            .also { schema ->
                val (newSchema, newSchemaTime) = measureTimedValue {
                    HideFieldsInEnvironmentTypeSupport.apply(schema)
                }
                println("Took ${newSchemaTime.inWholeMilliseconds}ms to transform SCHEMA to hide lifecycle fields")
                File("hidden-reference.graphqls").writeText(
                    SchemaPrinter().print(newSchema)
                )
            }
    }

    private fun getDefinitions(serviceRegistries: List<NadelTypeDefinitionRegistry>): List<AnySDLDefinition> {
        val topLevelFields = NadelOperationKind.entries
            .associateWith {
                mutableListOf<FieldDefinition>()
            }

        val allDefinitions = ArrayList<AnySDLDefinition>()

        for (definitionRegistry in serviceRegistries) {
            collectTypes(topLevelFields, allDefinitions, definitionRegistry)
        }

        // Create merged operation types in schema for all top level fields
        topLevelFields.keys.forEach { operationKind ->
            val fields = topLevelFields[operationKind]
            if (fields?.isNotEmpty() == true) {
                allDefinitions.add(
                    newObjectTypeDefinition()
                        .name(operationKind.defaultTypeName)
                        .sourceLocation(SourceLocation(-1, -1, "Generated"))
                        .fieldDefinitions(fields)
                        .build()
                )
            }
        }

        // add our custom directives if they are not present
        addIfNotPresent(allDefinitions, NadelDirectives.nadelHydrationArgumentDefinition)
        addIfNotPresent(allDefinitions, NadelDirectives.hydratedDirectiveDefinition)
        addIfNotPresent(allDefinitions, NadelDirectives.virtualTypeDirectiveDefinition)
        addIfNotPresent(allDefinitions, NadelDirectives.defaultHydrationDirectiveDefinition)
        addIfNotPresent(allDefinitions, NadelDirectives.idHydratedDirectiveDefinition)
        addIfNotPresent(allDefinitions, NadelDirectives.stubbedDirectiveDefinition)
        addIfNotPresent(allDefinitions, NadelDirectives.renamedDirectiveDefinition)
        addIfNotPresent(allDefinitions, NadelDirectives.hiddenDirectiveDefinition)
        addIfNotPresent(allDefinitions, NadelDirectives.nadelBatchObjectIdentifiedByDefinition)
        addIfNotPresent(allDefinitions, NadelDirectives.nadelHydrationResultFieldPredicateDefinition)
        addIfNotPresent(allDefinitions, NadelDirectives.nadelHydrationResultConditionDefinition)
        addIfNotPresent(allDefinitions, NadelDirectives.nadelHydrationConditionDefinition)
        addIfNotPresent(allDefinitions, NadelDirectives.nadelHydrationRemainingArguments)
        addIfNotPresent(allDefinitions, NadelDirectives.deferDirectiveDefinition)
        addIfNotPresent(allDefinitions, NadelDirectives.namespacedDirectiveDefinition)
        addIfNotPresent(allDefinitions, NadelDirectives.partitionDirectiveDefinition)

        addIfNotPresent(
            allDefinitions, newScalarTypeDefinition()
                .name(ExtendedScalars.Json.name)
                .build()
        )

        return allDefinitions
    }

    private fun createTypeRegistry(definitions: List<AnySDLDefinition>): TypeDefinitionRegistry {
        val overallRegistry = TypeDefinitionRegistry()

        for (definition in definitions) {
            val error = overallRegistry.add(definition)
            if (error.isPresent) {
                throw GraphQLException("Unable to add definition to overall registry: " + error.get().message)
            }
        }

        return overallRegistry
    }

    private inline fun <reified T : AnySDLNamedDefinition> addIfNotPresent(
        allDefinitions: MutableList<AnySDLDefinition>,
        namedDefinition: T,
    ) {
        if (!containsElement(allDefinitions, namedDefinition)) {
            allDefinitions.add(namedDefinition)
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
            // if it's an `extend type Foo` then it does not count since we need an actual `type Foo` defined
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
        definitionRegistry: NadelTypeDefinitionRegistry,
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
