package graphql.nadel.engine.transform.partition

import graphql.language.ScalarValue
import graphql.schema.GraphQLInputValueDefinition

/**
 * Interface to extract the partition key from a scalar value.
 */
interface NadelPartitionKeyExtractor {
    /**
     * Function to extract the partition key from the given scalar value.
     *
     * A `null` return value indicates that the scalar value should not be partitioned.
     */
    fun getPartitionKey(
        scalarValue: ScalarValue<*>,
        inputValueDef: GraphQLInputValueDefinition,
        context: NadelFieldPartitionContext,
    ): String?
}
