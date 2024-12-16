package graphql.nadel.engine.transform.query

import graphql.normalized.ExecutableNormalizedField

internal class NadelFieldNotFoundException(
    val field: ExecutableNormalizedField,
) : IllegalStateException(
    "Weirdly unable to look up routing for " + field.printDetails(),
)
