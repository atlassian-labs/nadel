package graphql.nadel.tests

import graphql.nadel.validation.NadelSchemaValidationError

data class ValidationException constructor(
    override val message: String,
    val errors: Set<NadelSchemaValidationError>,
) : RuntimeException(message)
