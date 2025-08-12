package graphql.nadel.tests.next.fixtures.partition.hooks

import graphql.language.ScalarValue
import graphql.language.StringValue
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.transform.partition.NadelPartitionFieldContext
import graphql.nadel.engine.transform.partition.NadelPartitionKeyExtractor
import graphql.nadel.engine.transform.partition.NadelPartitionTransformHook
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLInputValueDefinition

class RoutingBasedPartitionTransformHook : NadelPartitionTransformHook {
    override fun getFieldPartitionContext(
        operationExecutionContext: NadelOperationExecutionContext,
        overallField: ExecutableNormalizedField,
    ): NadelPartitionFieldContext {
        return object : NadelPartitionFieldContext() {}
    }

    override fun getPartitionKeyExtractor(): NadelPartitionKeyExtractor {
        return object: NadelPartitionKeyExtractor {
            override fun getPartitionKey(
                scalarValue: ScalarValue<*>,
                inputValueDef: GraphQLInputValueDefinition,
                context: NadelPartitionFieldContext,
            ): String? {
                return if (scalarValue !is StringValue) {
                    null
                } else {
                    if (scalarValue.value.contains(":").not()) {
                        null
                    } else {
                        scalarValue.value.split(":")[1]
                    }
                }
            }
        }
    }
}
