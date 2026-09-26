package graphql.nadel.error

import graphql.ErrorClassification
import graphql.GraphQLError
import graphql.language.SourceLocation

internal abstract class NadelGraphQLError(
    private val message: String,
    private val extensions: Map<String, Any?>,
    private val path: List<Any>? = null,
    private val errorClassification: ErrorClassification? = null,
) : GraphQLError {
    override fun getMessage(): String {
        return message
    }

    override fun getLocations(): MutableList<SourceLocation>? {
        return null
    }

    override fun getPath(): List<Any>? {
        return path
    }

    override fun getErrorType(): ErrorClassification {
        return errorClassification ?: ErrorClassification.errorClassification(javaClass.simpleName)
    }

    override fun getExtensions(): Map<String, Any?> {
        return extensions
    }
}
