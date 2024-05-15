package graphql.nadel.error

import graphql.ErrorClassification
import graphql.ErrorClassification.errorClassification

abstract class NadelGraphQLErrorException(
    message: String,
    private val path: List<Any>? = null,
    private val errorClassification: ErrorClassification? = null,
) : NadelGraphQLErrorExceptionKotlinCompat(message) {
    override fun getPath(): List<Any>? {
        return path
    }

    override fun getErrorType(): ErrorClassification {
        return errorClassification ?: errorClassification(javaClass.simpleName)
    }
}
