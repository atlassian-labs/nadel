package graphql.nadel.definition.coordinates

import graphql.nadel.engine.blueprint.NadelSchemaTraverser
import graphql.nadel.engine.blueprint.NadelSchemaTraverserElement
import graphql.nadel.engine.blueprint.NadelSchemaTraverserVisitor
import graphql.schema.GraphQLDirective
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLSchema
import graphql.schema.idl.DirectiveInfo
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

    private fun createImpl(
        roots: List<NadelSchemaTraverserElement>,
    ): Set<NadelSchemaMemberCoordinates> {
        val coordinates = mutableSetOf<NadelSchemaMemberCoordinates>()

        NadelSchemaTraverser()
            .traverse(
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
                        return false // Don't traverse argument further
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
