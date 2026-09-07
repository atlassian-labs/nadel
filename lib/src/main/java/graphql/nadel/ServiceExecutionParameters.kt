package graphql.nadel

import graphql.GraphQLContext
import graphql.execution.ExecutionId
import graphql.language.Document
import graphql.language.OperationDefinition
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.normalized.ExecutableNormalizedField

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
    /**
     * The overall-schema top-level fields grouped into this service execution.
     *
     * All fields in this list are assigned to the same service and sharding target.
     */
    val overallExecutableNormalizedFields: List<ExecutableNormalizedField>,
    /**
     * A representative top-level field after query transformation.
     *
     * Use [overallExecutableNormalizedFields] when every overall-schema field in this service
     * execution is required.
     */
    val executableNormalizedField: ExecutableNormalizedField,
) {
    val isHydrationCall: Boolean
        get() = hydrationDetails != null
}
