package graphql.nadel.schema

internal class NadelSchemaDefinitionTraverser {
    fun traverse(
        roots: Iterable<NadelSchemaDefinitionTraverserElement>,
        visitor: NadelSchemaDefinitionTraverserVisitor,
    ) {
        val queue: MutableList<NadelSchemaDefinitionTraverserElement> = roots.toMutableList()

        val visitedRoots: MutableSet<String> = roots
            .mapNotNullTo(mutableSetOf()) {
                if (it is NadelSchemaDefinitionTraverserElement.Type) {
                    it.node.name
                } else {
                    null
                }
            }

        val addToQueue = fun(element: NadelSchemaDefinitionTraverserElement) {
            if (element is NadelSchemaDefinitionTraverserElement.Type) {
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
                is NadelSchemaDefinitionTraverserElement.AppliedDirective -> {
                    visitor.visitGraphQLAppliedDirective(element)
                }
                is NadelSchemaDefinitionTraverserElement.AppliedDirectiveArgument -> {
                    visitor.visitGraphQLAppliedDirectiveArgument(element)
                }
                is NadelSchemaDefinitionTraverserElement.Argument -> {
                    visitor.visitGraphQLArgument(element)
                }
                is NadelSchemaDefinitionTraverserElement.Directive -> {
                    visitor.visitGraphQLDirective(element)
                }
                is NadelSchemaDefinitionTraverserElement.EnumType -> {
                    visitor.visitGraphQLEnumType(element)
                }
                is NadelSchemaDefinitionTraverserElement.EnumValueDefinition -> {
                    visitor.visitGraphQLEnumValueDefinition(element)
                }
                is NadelSchemaDefinitionTraverserElement.FieldDefinition -> {
                    visitor.visitGraphQLFieldDefinition(element)
                }
                is NadelSchemaDefinitionTraverserElement.InputObjectField -> {
                    visitor.visitGraphQLInputObjectField(element)
                }
                is NadelSchemaDefinitionTraverserElement.InputObjectType -> {
                    visitor.visitGraphQLInputObjectType(element)
                }
                is NadelSchemaDefinitionTraverserElement.ScalarType -> {
                    visitor.visitGraphQLScalarType(element)
                }
                is NadelSchemaDefinitionTraverserElement.InterfaceType -> {
                    visitor.visitGraphQLInterfaceType(element)
                }
                is NadelSchemaDefinitionTraverserElement.ObjectType -> {
                    visitor.visitGraphQLObjectType(element)
                }
                is NadelSchemaDefinitionTraverserElement.UnionType -> {
                    visitor.visitGraphQLUnionType(element)
                }
                is NadelSchemaDefinitionTraverserElement.TypeReference -> {
                    visitor.visitTypeReference(element)
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
