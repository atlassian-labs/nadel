package graphql.nadel.engine.transform.partition

import graphql.ErrorClassification
import graphql.nadel.error.NadelGraphQLErrorException

class NadelPartitionGraphQLErrorException(
    message: String,
    path: List<Any>? = null,
    errorClassification: ErrorClassification? = partitioningErrorClassification,
) : NadelGraphQLErrorException(message, path, errorClassification)

class NadelCannotPartitionFieldException(
    message: String,
) : Exception(message)

val partitioningErrorClassification: ErrorClassification = ErrorClassification.errorClassification("PartitioningError")
