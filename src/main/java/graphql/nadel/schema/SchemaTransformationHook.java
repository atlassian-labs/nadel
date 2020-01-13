package graphql.nadel.schema;

import graphql.PublicSpi;
import graphql.schema.GraphQLSchema;

/**
 * High level representation of a transformation for an overall (not underlying) schema. Warning: the schema resulting
 * from the transformation is not validated by Nadel, so may produce unpredictable results if incorrect.
 *
 * <p>Example usage, to delete a field:
 * <code>
 *     SchemaTransformation transformation = originalSchema -{@literal >} {
 *         {@literal @}Override
 *         GraphQLSchema apply(GraphQLSchema originalSchema) {
 *             return SchemaTransformer.transformSchema(originalSchema, new GraphQLTypeVisitorStub() {
 *                  {@literal @}Override
 *                  TraversalControl visitGraphQLFieldDefinition(GraphQLFieldDefinition node, TraverserContext{@literal <}GraphQLSchemaElement{@literal >} context) {
 *                      if (node.getName() == "secretField") {
 *                          return TreeTransformerUtil.deleteNode(node);
 *                      }
 *
 *                      return TraversalControl.CONTINUE;
 *                  }
 *             }
 *         }
 *     }
 * </code>
 *
 * @see graphql.schema.SchemaTransformer
 * @see graphql.schema.GraphQLTypeVisitorStub
 */
@PublicSpi
@FunctionalInterface
public interface SchemaTransformationHook {

    SchemaTransformationHook IDENTITY = originalSchema -> originalSchema; // no-op

    /**
     * Apply a transformation to a schema object, returning the new schema.
     *
     * @param originalSchema input schema
     * @return transformed schema
     */
    GraphQLSchema apply(GraphQLSchema originalSchema);

}
