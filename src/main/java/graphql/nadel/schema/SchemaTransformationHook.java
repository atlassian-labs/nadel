package graphql.nadel.schema;

import graphql.PublicSpi;
import graphql.schema.GraphQLSchema;

/**
 * High level representation of a a graphql schema transformation. Warning: the schema resulting from the transformation
 * is not validated by Nadel, so may produce unpredictable results if incorrect.
 *
 * <p>Example usage, to delete a field:
 * <code>
 *     SchemaTransformation transformation = new SchemaTransformation() {
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
public interface SchemaTransformationHook {

    /**
     * Apply a transformation to a schema object, returning the new schema.
     *
     * @param originalSchema input schema
     * @return transformed schema
     */
    default GraphQLSchema apply(GraphQLSchema originalSchema) {
        return originalSchema; // no-op
    }

}
