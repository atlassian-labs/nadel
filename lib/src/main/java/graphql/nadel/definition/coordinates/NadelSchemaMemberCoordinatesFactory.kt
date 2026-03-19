package graphql.nadel.definition.coordinates

import graphql.Directives
import graphql.language.Document
import graphql.language.NamedNode
import graphql.nadel.engine.util.AnySDLDefinition
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.schema.NadelSchemaDefinitionTraverser
import graphql.nadel.schema.NadelSchemaDefinitionTraverserElement
import graphql.nadel.schema.NadelSchemaDefinitionTraverserVisitor
import graphql.nadel.schema.NadelSchemaTraverser
import graphql.nadel.schema.NadelSchemaTraverserElement
import graphql.nadel.schema.NadelSchemaTraverserVisitor
import graphql.schema.GraphQLDirective
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLSchema
import graphql.schema.idl.ScalarInfo

class NadelSchemaMemberCoordinatesFactory {
    fun create(
        schema: GraphQLSchema,
    ): Set<NadelSchemaMemberCoordinates> {
        val roots = buildList {
            fun addGraphQLType(type: GraphQLNamedType?) {
                if (type != null) {
                    add(NadelSchemaTraverserElement.from(type))
                }
            }

            fun addGraphQLDirective(directive: GraphQLDirective?) {
                if (directive != null) {
                    add(NadelSchemaTraverserElement.from(directive))
                }
            }

            addGraphQLType(schema.queryType)
            addGraphQLType(schema.mutationType)
            addGraphQLType(schema.subscriptionType)

            schema.additionalTypes.forEach { type ->
                if (type is GraphQLNamedType) {
                    addGraphQLType(type)
                }
            }
            schema.directives.forEach { directive ->
                addGraphQLDirective(directive)
            }
        }

        return createImpl(roots)
    }

    fun create(
        roots: List<GraphQLNamedType>,
    ): Set<NadelSchemaMemberCoordinates> {
        return createImpl(
            roots = roots.map {
                NadelSchemaTraverserElement.from(it)
            },
        )
    }

    fun create(
        schema: Document,
    ): Set<NadelSchemaMemberCoordinates> {
        val definitions = schema.definitions
            .asSequence()
            .filterIsInstance<AnySDLDefinition>()

        // There can be multiple definitions per name, but in this scenario we don't care
        val definitionByName = definitions
            .associateBy {
                (it as NamedNode<*>).name
            }

        val roots = definitions
            .mapNotNull(NadelSchemaDefinitionTraverserElement::from)
            .toList()

        return createImpl(roots, definitionByName)
    }

    private fun createImpl(
        roots: List<NadelSchemaTraverserElement>,
    ): Set<NadelSchemaMemberCoordinates> {
        val coordinates = mutableSetOf<NadelSchemaMemberCoordinates>()

        NadelSchemaTraverser()
            .traverse(
                roots,
                NadelSchemaCoordinateCollectorTraverserVisitor(coordinates),
            )

        return coordinates
    }

    private fun createImpl(
        roots: List<NadelSchemaDefinitionTraverserElement>,
        definitionByName: Map<String, AnySDLDefinition>,
    ): Set<NadelSchemaMemberCoordinates> {
        val coordinates = mutableSetOf<NadelSchemaMemberCoordinates>()

        NadelSchemaDefinitionTraverser()
            .traverse(
                roots,
                NadelSchemaDefinitionCoordinateCollectorTraverserVisitor(coordinates, definitionByName),
            )

        return coordinates
    }
}

internal class NadelSchemaCoordinateCollectorTraverserVisitor(
    private val coordinates: MutableCollection<NadelSchemaMemberCoordinates>,
) : NadelSchemaTraverserVisitor {
    override fun visitGraphQLArgument(
        element: NadelSchemaTraverserElement.Argument,
    ): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLUnionType(
        element: NadelSchemaTraverserElement.UnionType,
    ): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLUnionMemberType(
        element: NadelSchemaTraverserElement.UnionMemberType,
    ): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLInterfaceType(
        element: NadelSchemaTraverserElement.InterfaceType,
    ): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLEnumType(
        element: NadelSchemaTraverserElement.EnumType,
    ): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLEnumValueDefinition(
        element: NadelSchemaTraverserElement.EnumValueDefinition,
    ): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLFieldDefinition(
        element: NadelSchemaTraverserElement.FieldDefinition,
    ): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLInputObjectField(
        element: NadelSchemaTraverserElement.InputObjectField,
    ): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLInputObjectType(
        element: NadelSchemaTraverserElement.InputObjectType,
    ): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLObjectType(
        element: NadelSchemaTraverserElement.ObjectType,
    ): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLScalarType(
        element: NadelSchemaTraverserElement.ScalarType,
    ): Boolean {
        // Ignore built in scalars
        if (ScalarInfo.isGraphqlSpecifiedScalar(element.node.name)) {
            return false
        }

        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLDirective(
        element: NadelSchemaTraverserElement.Directive,
    ): Boolean {
        // Ignore built in directives
        if (Directives.isBuiltInDirective(element.node.name)) {
            return false
        }

        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLAppliedDirective(
        element: NadelSchemaTraverserElement.AppliedDirective,
    ): Boolean {
        coordinates.add(element.coordinates())
        return false // Don't traverse argument further
    }

    override fun visitGraphQLAppliedDirectiveArgument(
        element: NadelSchemaTraverserElement.AppliedDirectiveArgument,
    ): Boolean {
        coordinates.add(element.coordinates())
        return true
    }
}

internal class NadelSchemaDefinitionCoordinateCollectorTraverserVisitor(
    private val coordinates: MutableCollection<NadelSchemaMemberCoordinates>,
    private val definitionByName: Map<String, AnySDLDefinition>,
) : NadelSchemaDefinitionTraverserVisitor {
    override fun visitGraphQLAppliedDirective(element: NadelSchemaDefinitionTraverserElement.AppliedDirective): Boolean {
        coordinates.add(element.coordinates())
        return false // Don't traverse argument further
    }

    override fun visitGraphQLAppliedDirectiveArgument(element: NadelSchemaDefinitionTraverserElement.AppliedDirectiveArgument): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLArgument(element: NadelSchemaDefinitionTraverserElement.Argument): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLDirective(element: NadelSchemaDefinitionTraverserElement.Directive): Boolean {
        // Ignore built in directives
        if (Directives.isBuiltInDirective(element.node.name)) {
            return false
        }

        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLEnumType(element: NadelSchemaDefinitionTraverserElement.EnumType): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLEnumValueDefinition(element: NadelSchemaDefinitionTraverserElement.EnumValueDefinition): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLFieldDefinition(element: NadelSchemaDefinitionTraverserElement.FieldDefinition): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLInputObjectField(element: NadelSchemaDefinitionTraverserElement.InputObjectField): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLInputObjectType(element: NadelSchemaDefinitionTraverserElement.InputObjectType): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLInterfaceType(element: NadelSchemaDefinitionTraverserElement.InterfaceType): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLObjectType(element: NadelSchemaDefinitionTraverserElement.ObjectType): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLScalarType(element: NadelSchemaDefinitionTraverserElement.ScalarType): Boolean {
        // Ignore built in scalars
        if (ScalarInfo.isGraphqlSpecifiedScalar(element.node.name)) {
            return false
        }

        coordinates.add(element.coordinates())
        return true
    }

    override fun visitGraphQLUnionType(element: NadelSchemaDefinitionTraverserElement.UnionType): Boolean {
        coordinates.add(element.coordinates())
        return true
    }

    override fun visitTypeReference(element: NadelSchemaDefinitionTraverserElement.TypeReference): Boolean {
        // Resolve definition then traverse
        val typeName = element.node.unwrapAll().name

        // Can't resolve built in scalars
        if (ScalarInfo.isGraphqlSpecifiedScalar(typeName) || Directives.isBuiltInDirective(typeName)) {
            return false
        }

        val definition = definitionByName[typeName] ?: throw NullPointerException(typeName)
        val resolvedElement = NadelSchemaDefinitionTraverserElement.from(definition) ?: return false
        return visitElement(resolvedElement)
    }

    private fun visitElement(element: NadelSchemaDefinitionTraverserElement): Boolean {
        return when (element) {
            is NadelSchemaDefinitionTraverserElement.AppliedDirective ->
                visitGraphQLAppliedDirective(element)
            is NadelSchemaDefinitionTraverserElement.AppliedDirectiveArgument ->
                visitGraphQLAppliedDirectiveArgument(element)
            is NadelSchemaDefinitionTraverserElement.DirectiveArgument ->
                visitGraphQLArgument(element)
            is NadelSchemaDefinitionTraverserElement.FieldArgument ->
                visitGraphQLArgument(element)
            is NadelSchemaDefinitionTraverserElement.EnumType ->
                visitGraphQLEnumType(element)
            is NadelSchemaDefinitionTraverserElement.EnumValueDefinition ->
                visitGraphQLEnumValueDefinition(element)
            is NadelSchemaDefinitionTraverserElement.FieldDefinition ->
                visitGraphQLFieldDefinition(element)
            is NadelSchemaDefinitionTraverserElement.InputObjectField ->
                visitGraphQLInputObjectField(element)
            is NadelSchemaDefinitionTraverserElement.InputObjectType ->
                visitGraphQLInputObjectType(element)
            is NadelSchemaDefinitionTraverserElement.InterfaceType ->
                visitGraphQLInterfaceType(element)
            is NadelSchemaDefinitionTraverserElement.ObjectType ->
                visitGraphQLObjectType(element)
            is NadelSchemaDefinitionTraverserElement.ScalarType ->
                visitGraphQLScalarType(element)
            is NadelSchemaDefinitionTraverserElement.UnionType ->
                visitGraphQLUnionType(element)
            is NadelSchemaDefinitionTraverserElement.Directive ->
                visitGraphQLDirective(element)
            is NadelSchemaDefinitionTraverserElement.TypeReference ->
                visitTypeReference(element)
        }
    }
}
