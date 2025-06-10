package graphql.nadel.validation

import graphql.nadel.definition.NadelInstructionDefinition
import graphql.nadel.definition.coordinates.NadelSchemaMemberCoordinates
import graphql.nadel.definition.coordinates.coordinates
import graphql.nadel.definition.hydration.parseDefaultHydrationOrNull
import graphql.nadel.definition.hydration.parseHydrationDefinitions
import graphql.nadel.definition.partition.parsePartitionOrNull
import graphql.nadel.definition.renamed.parseRenamedOrNull
import graphql.nadel.definition.stubbed.parseStubbedOrNull
import graphql.nadel.definition.virtualType.NadelVirtualTypeDefinition
import graphql.nadel.definition.virtualType.hasVirtualTypeDefinition
import graphql.nadel.engine.blueprint.NadelSchemaTraverser
import graphql.nadel.engine.blueprint.NadelSchemaTraverserElement
import graphql.nadel.engine.blueprint.NadelSchemaTraverserVisitor
import graphql.schema.GraphQLSchema

internal class NadelInstructionDefinitionParser(
    private val hook: NadelSchemaValidationHook,
    private val idHydrationDefinitionParser: NadelIdHydrationDefinitionParser,
) {
    fun parse(
        engineSchema: GraphQLSchema,
    ): NadelValidationInterimResult<NadelInstructionDefinitionRegistry> {
        val definitionMap = mutableMapOf<NadelSchemaMemberCoordinates, MutableList<NadelInstructionDefinition>>()
        val errors = mutableListOf<NadelSchemaValidationResult>()

        NadelSchemaTraverser()
            .traverse(
                engineSchema,
                engineSchema.typeMap.keys,
                object : NadelSchemaTraverserVisitor {
                    override fun visitGraphQLArgument(
                        element: NadelSchemaTraverserElement.Argument,
                    ): Boolean {
                        return true
                    }

                    override fun visitGraphQLUnionType(
                        element: NadelSchemaTraverserElement.UnionType,
                    ): Boolean {
                        visitType(element)
                        return true
                    }

                    override fun visitGraphQLUnionMemberType(
                        element: NadelSchemaTraverserElement.UnionMemberType,
                    ): Boolean {
                        return true
                    }

                    override fun visitGraphQLInterfaceType(
                        element: NadelSchemaTraverserElement.InterfaceType,
                    ): Boolean {
                        visitType(element)
                        return true
                    }

                    override fun visitGraphQLEnumType(
                        element: NadelSchemaTraverserElement.EnumType,
                    ): Boolean {
                        visitType(element)
                        return true
                    }

                    override fun visitGraphQLEnumValueDefinition(
                        element: NadelSchemaTraverserElement.EnumValueDefinition,
                    ): Boolean {
                        return true
                    }

                    override fun visitGraphQLFieldDefinition(
                        element: NadelSchemaTraverserElement.FieldDefinition,
                    ): Boolean {
                        val parent = element.parent
                        val node = element.node

                        val coords by lazy {
                            parent.coordinates().field(node.name)
                        }

                        fun addAll(defs: List<NadelInstructionDefinition>) {
                            definitionMap.computeIfAbsent(coords) { mutableListOf() }.addAll(defs)
                        }

                        // todo: I think we can clean this up if we make all these extend from a new super NadelFieldDefinitionParser
                        // e.g. for (parser in parsers) addAll(parser.parse(field))

                        val rename = node.parseRenamedOrNull()
                        if (rename != null) {
                            addAll(listOf(rename))
                        }

                        val stub = node.parseStubbedOrNull()
                        if (stub != null) {
                            addAll(listOf(stub))
                        }

                        val partition = node.parsePartitionOrNull()
                        if (partition != null) {
                            addAll(listOf(partition))
                        }

                        val hydrations = node.parseHydrationDefinitions()
                        if (hydrations.isNotEmpty()) {
                            addAll(hydrations)
                        }

                        val customDefinitions = hook.parseDefinitions(engineSchema, parent, node)
                        if (customDefinitions.isNotEmpty()) {
                            addAll(customDefinitions)
                        }

                        val idHydrations = idHydrationDefinitionParser.parse(parent, node)
                            .onError {
                                errors.addAll(it.results)
                                return false
                            }
                        if (idHydrations.isNotEmpty()) {
                            addAll(idHydrations)
                        }

                        return true
                    }

                    override fun visitGraphQLInputObjectField(
                        element: NadelSchemaTraverserElement.InputObjectField,
                    ): Boolean {
                        return true
                    }

                    override fun visitGraphQLInputObjectType(
                        element: NadelSchemaTraverserElement.InputObjectType,
                    ): Boolean {
                        visitType(element)
                        return true
                    }

                    override fun visitGraphQLObjectType(
                        element: NadelSchemaTraverserElement.ObjectType,
                    ): Boolean {
                        visitType(element)
                        return true
                    }

                    override fun visitGraphQLScalarType(
                        element: NadelSchemaTraverserElement.ScalarType,
                    ): Boolean {
                        visitType(element)
                        return true
                    }

                    override fun visitGraphQLDirective(
                        element: NadelSchemaTraverserElement.Directive,
                    ): Boolean {
                        return true
                    }

                    override fun visitGraphQLAppliedDirective(
                        element: NadelSchemaTraverserElement.AppliedDirective,
                    ): Boolean {
                        return false
                    }

                    override fun visitGraphQLAppliedDirectiveArgument(
                        element: NadelSchemaTraverserElement.AppliedDirectiveArgument,
                    ): Boolean {
                        return false
                    }

                    private fun visitType(element: NadelSchemaTraverserElement.Type) {
                        val coordinates = element.coordinates()
                        val type = element.node

                        val definitions by lazy {
                            definitionMap.computeIfAbsent(coordinates) {
                                mutableListOf()
                            }
                        }

                        type.parseRenamedOrNull()?.also(definitions::add)
                        type.parseDefaultHydrationOrNull()?.also(definitions::add)
                        if (type.hasVirtualTypeDefinition()) {
                            definitions.add(NadelVirtualTypeDefinition())
                        }
                    }
                }
            )

        return if (errors.isEmpty()) {
            NadelValidationInterimResult.Success.of(NadelInstructionDefinitionRegistry(definitionMap))
        } else {
            NadelValidationInterimResult.Error.of(NadelSchemaValidationResults(errors))
        }
    }
}
