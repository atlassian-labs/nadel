package graphql.nadel.engine.transform.partition

import graphql.ErrorClassification
import graphql.nadel.error.NadelGraphQLErrorException

class NadelPartitionException(
    message: String,
    path: List<Any>? = null,
    errorClassification: ErrorClassification? = null,
) : NadelGraphQLErrorException(message, path, errorClassification)

class NadelCannotPartitionFieldException(
    message: String,
) : Exception(message)

