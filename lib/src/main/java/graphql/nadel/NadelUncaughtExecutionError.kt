package graphql.nadel

import graphql.nadel.error.NadelGraphQLError

internal class NadelUncaughtExecutionError(
    message: String,
    extensions: Map<String, Any?>,
    val cause: Throwable,
) : NadelGraphQLError(
    message = message,
    extensions = extensions,
)
