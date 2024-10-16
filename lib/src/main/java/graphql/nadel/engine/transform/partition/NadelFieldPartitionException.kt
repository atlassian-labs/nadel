package graphql.nadel.engine.transform.partition

import graphql.ErrorClassification
import graphql.nadel.error.NadelGraphQLErrorException

class NadelPartitionGraphQLErrorException(
    message: String,
    path: List<Any>? = null,
) : NadelGraphQLErrorException(message, path, partitioningErrorClassification)

class NadelCannotPartitionFieldException(
    message: String,
) : Exception(message)

val partitioningErrorClassification: ErrorClassification = ErrorClassification.errorClassification("PartitioningError")
