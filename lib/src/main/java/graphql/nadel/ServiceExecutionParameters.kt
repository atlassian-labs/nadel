package graphql.nadel

import graphql.GraphQLContext
import graphql.execution.ExecutionId
import graphql.introspection.Introspection.TypeNameMetaFieldDef
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
     * All transformed underlying-schema root fields represented by [query], including artificial fields.
     */
    val executableNormalizedFields: List<ExecutableNormalizedField>,
) {
    /**
     * The first non-__typename underlying field, or the first field if all are __typename.
     * Retains the legacy selection behavior; additional service-call fields are omitted.
     */
    @Deprecated(
        message = "Use executableNormalizedFields and explicitly handle all fields in the service call.",
    )
    val executableNormalizedField: ExecutableNormalizedField =
        executableNormalizedFields.firstOrNull { it.fieldName != TypeNameMetaFieldDef.name }
            ?: executableNormalizedFields.first()

    val isHydrationCall: Boolean
        get() = hydrationDetails != null
}
