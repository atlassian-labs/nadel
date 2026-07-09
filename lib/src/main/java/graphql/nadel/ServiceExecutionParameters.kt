package graphql.nadel

import graphql.GraphQLContext
import graphql.execution.ExecutionId
import graphql.language.Document
import graphql.language.OperationDefinition
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.compiler.NadelExecutableNormalizedField

class ServiceExecutionParameters internal constructor(
    val query: Document,
    val context: Any?,
    val graphQLContext: GraphQLContext,
    val variables: Map<String, Any>,
    val operationDefinition: OperationDefinition,
    val executionId: ExecutionId,
    val serviceExecutionContext: NadelServiceExecutionContext,
    /**
     * @return details abut this service hydration or null if it's not a hydration call
     */
    val hydrationDetails: ServiceExecutionHydrationDetails?,
    val executableNormalizedField: NadelExecutableNormalizedField,
) {
    val isHydrationCall: Boolean
        get() = hydrationDetails != null
}
