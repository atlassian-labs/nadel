package graphql.nadel.schema

import graphql.nadel.Service
import graphql.schema.GraphQLSchema

/**
 * High level representation of a transformation for an overall (not underlying) schema. Warning: the schema resulting
 * from the transformation is not validated by Nadel, so may produce unpredictable results if incorrect.
 *
 *
 * Example usage, to delete a field:
 * `
 * SchemaTransformation transformation = originalSchema -> {
 * @Override
 * GraphQLSchema apply(GraphQLSchema originalSchema) {
 * return SchemaTransformer.transformSchema(originalSchema, new GraphQLTypeVisitorStub() {
 * @Override
 * TraversalControl visitGraphQLFieldDefinition(GraphQLFieldDefinition node, TraverserContext<GraphQLSchemaElement> context) {
 * if (node.getName() == "secretField") {
 * return TreeTransformerUtil.deleteNode(node);
 * }
 *
 * return TraversalControl.CONTINUE;
 * }
 * }
 * }
 * }
` *
 *
 * @see graphql.schema.SchemaTransformer
 *
 * @see graphql.schema.GraphQLTypeVisitorStub
 */
fun interface SchemaTransformationHook {
    /**
     * Apply a transformation to a schema object, returning the new schema.
     *
     * @param originalSchema input schema
     * @param services       the list of Nadel services
     *
     * @return transformed schema
     */
    fun apply(originalSchema: GraphQLSchema, services: List<Service>): GraphQLSchema

    companion object {
        val Identity = SchemaTransformationHook { originalSchema, _ -> originalSchema }
    }
}
