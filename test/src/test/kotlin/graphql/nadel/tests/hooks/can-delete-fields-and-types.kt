package graphql.nadel.tests.hooks

import graphql.nadel.schema.SchemaTransformationHook
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchemaElement
import graphql.schema.GraphQLTypeVisitorStub
import graphql.schema.SchemaTransformer.transformSchema
import graphql.util.TraversalControl
import graphql.util.TraverserContext

@UseHook
class `can-delete-fields-and-types` : EngineTestHook {
    override val schemaTransformationHook = SchemaTransformationHook { originalSchema, _ ->
        transformSchema(originalSchema, object : GraphQLTypeVisitorStub() {
            override fun visitGraphQLFieldDefinition(
                node: GraphQLFieldDefinition,
                context: TraverserContext<GraphQLSchemaElement>,
            ): TraversalControl {
                if (node.name == "foo") {
                    return deleteNode(context)
                }
                return super.visitGraphQLFieldDefinition(node, context)
            }

            override fun visitGraphQLObjectType(
                node: GraphQLObjectType,
                context: TraverserContext<GraphQLSchemaElement>,
            ): TraversalControl {
                if (node.name == "Foo") {
                    return deleteNode(context)
                }
                return super.visitGraphQLObjectType(node, context)
            }
        })
    }
}
