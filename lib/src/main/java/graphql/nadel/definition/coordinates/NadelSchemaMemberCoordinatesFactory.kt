package graphql.nadel.definition.coordinates

import graphql.nadel.engine.blueprint.NadelSchemaTraverser
import graphql.nadel.engine.blueprint.NadelSchemaTraverserElement
import graphql.nadel.engine.blueprint.NadelSchemaTraverserVisitor
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLSchema
import graphql.schema.idl.DirectiveInfo
import graphql.schema.idl.ScalarInfo

class NadelSchemaMemberCoordinatesFactory {
    fun create(
        schema: GraphQLSchema,
    ): Set<NadelSchemaMemberCoordinates> {
        val roots = buildList {
            add(schema.queryType.name)
            val mutationType = schema.mutationType
            if (mutationType != null) {
                add(mutationType.name)
            }
            val subscriptionType = schema.subscriptionType
            if (subscriptionType != null) {
                add(subscriptionType.name)
            }
            schema.additionalTypes.forEach { type ->
                if (type is GraphQLNamedType) {
                    add(type.name)
                }
            }
            schema.directives.forEach { directive ->
                add(directive.name)
            }
        }

        return createImpl(schema, roots)
    }

    fun create(
        schema: GraphQLSchema,
        roots: List<GraphQLNamedSchemaElement>,
    ): Set<NadelSchemaMemberCoordinates> {
        return createImpl(
            schema,
            roots = roots.map { it.name },
        )
    }

    private fun createImpl(
        schema: GraphQLSchema,
        roots: List<String>,
    ): Set<NadelSchemaMemberCoordinates> {
        val coordinates = mutableSetOf<NadelSchemaMemberCoordinates>()

        NadelSchemaTraverser()
            .traverse(
                schema,
                roots,
                object : NadelSchemaTraverserVisitor {
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
                        if (DirectiveInfo.isGraphqlSpecifiedDirective(element.node.name)) {
                            return false
                        }

                        coordinates.add(element.coordinates())
                        return true
                    }

                    override fun visitGraphQLAppliedDirective(
                        element: NadelSchemaTraverserElement.AppliedDirective,
                    ): Boolean {
                        coordinates.add(element.coordinates())
                        return true
                    }

                    override fun visitGraphQLAppliedDirectiveArgument(
                        element: NadelSchemaTraverserElement.AppliedDirectiveArgument,
                    ): Boolean {
                        coordinates.add(element.coordinates())
                        return true
                    }
                }
            )

        return coordinates
    }
}
