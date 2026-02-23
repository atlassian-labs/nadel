package graphql.nadel.engine.blueprint

import graphql.schema.GraphQLSchema

/**
 * Significantly faster than normal [graphql.schema.SchemaTraverser] as it's simpler.
 *
 * Validating central schema with old [graphql.schema.SchemaTraverser] takes 306ms and this impl takes 74ms.
 *
 * That's 4x faster.
 */
internal class NadelSchemaTraverser {
    fun traverse(
        schema: GraphQLSchema,
        roots: Iterable<String>,
        visitor: NadelSchemaTraverserVisitor,
    ) {
        val rootsResolved: MutableList<NadelSchemaTraverserElement> = roots
            .mapNotNullTo(mutableListOf()) { typeName ->
                val type = schema.typeMap[typeName]
                // Types can be deleted by transformer, so they may not exist in end schema
                if (type == null) {
                    val directive = schema.getDirective(typeName)
                    if (directive == null) {
                        null
                    } else {
                        NadelSchemaTraverserElement.from(directive)
                    }
                } else {
                    NadelSchemaTraverserElement.from(type)
                }
            }

        return traverse(rootsResolved, visitor)
    }

    fun traverse(
        roots: Iterable<NadelSchemaTraverserElement>,
        visitor: NadelSchemaTraverserVisitor,
    ) {
        val queue: MutableList<NadelSchemaTraverserElement> = roots.toMutableList()

        val visitedRoots: MutableSet<String> = roots
            .mapNotNullTo(mutableSetOf()) {
                if (it is NadelSchemaTraverserElement.Type) {
                    it.node.name
                } else {
                    null
                }
            }

        val addToQueue = fun(element: NadelSchemaTraverserElement) {
            if (element is NadelSchemaTraverserElement.Type) {
                if (visitedRoots.add(element.node.name)) {
                    queue.add(element)
                }
            } else {
                queue.add(element)
            }
        }

        while (queue.isNotEmpty()) {
            val element = queue.removeLast()

            val result = when (element) {
                is NadelSchemaTraverserElement.AppliedDirective -> {
                    visitor.visitGraphQLAppliedDirective(element)
                }
                is NadelSchemaTraverserElement.AppliedDirectiveArgument -> {
                    visitor.visitGraphQLAppliedDirectiveArgument(element)
                }
                is NadelSchemaTraverserElement.Argument -> {
                    visitor.visitGraphQLArgument(element)
                }
                is NadelSchemaTraverserElement.Directive -> {
                    visitor.visitGraphQLDirective(element)
                }
                is NadelSchemaTraverserElement.EnumType -> {
                    visitor.visitGraphQLEnumType(element)
                }
                is NadelSchemaTraverserElement.EnumValueDefinition -> {
                    visitor.visitGraphQLEnumValueDefinition(element)
                }
                is NadelSchemaTraverserElement.FieldDefinition -> {
                    visitor.visitGraphQLFieldDefinition(element)
                }
                is NadelSchemaTraverserElement.InputObjectField -> {
                    visitor.visitGraphQLInputObjectField(element)
                }
                is NadelSchemaTraverserElement.InputObjectType -> {
                    visitor.visitGraphQLInputObjectType(element)
                }
                is NadelSchemaTraverserElement.ScalarType -> {
                    visitor.visitGraphQLScalarType(element)
                }
                is NadelSchemaTraverserElement.InterfaceType -> {
                    visitor.visitGraphQLInterfaceType(element)
                }
                is NadelSchemaTraverserElement.ObjectType -> {
                    visitor.visitGraphQLObjectType(element)
                }
                is NadelSchemaTraverserElement.UnionType -> {
                    visitor.visitGraphQLUnionType(element)
                }
                is NadelSchemaTraverserElement.UnionMemberType -> {
                    visitor.visitGraphQLUnionMemberType(element)
                }
            }

            if (!result) {
                continue
            }

            // Handle next
            element.forEachChild(addToQueue)
        }
    }
}
