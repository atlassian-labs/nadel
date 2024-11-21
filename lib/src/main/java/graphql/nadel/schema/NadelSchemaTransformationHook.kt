package graphql.nadel.schema

import graphql.nadel.Service
import graphql.schema.GraphQLSchema

/**
 * High level representation of a transformation for an overall (not underlying) schema.
 *
 * Warning: the schema resulting from the transformation is not validated by Nadel.
 * So may produce unpredictable results if incorrect.
 *
 * Example usage, to delete a field:
 *
 * ```kotlin
 * val transformation = SchemaTransformationHook { originalSchema, services ->
 *     SchemaTransformer.transformSchema(
 *         originalSchema,
 *         object : GraphQLTypeVisitorStub() {
 *             override fun visitGraphQLFieldDefinition(
 *                 node: GraphQLFieldDefinition,
 *                 context: TraverserContext<GraphQLSchemaElement>,
 *             ): TraversalControl {
 *                 if (node.name == "secretField") {
 *                     return TreeTransformerUtil.deleteNode(context)
 *                 }
 *                 return TraversalControl.CONTINUE
 *             }
 *         }
 *     )
 * }
 * ```
 *
 * @see graphql.schema.SchemaTransformer
 * @see graphql.schema.GraphQLTypeVisitorStub
 */
fun interface NadelSchemaTransformationHook {
    /**
     * Apply a transformation to a schema object, returning the new schema.
     *
     * @param originalSchema input schema
     * @param services       the list of Nadel services
     *
     * @return transformed schema
     */
    fun transform(originalSchema: GraphQLSchema, services: List<Service>): GraphQLSchema

    companion object {
        internal val Identity = NadelSchemaTransformationHook { originalSchema, _ -> originalSchema }
    }
}
