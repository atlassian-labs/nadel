package graphql.nadel.util

import graphql.ErrorType
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.language.SourceLocation

typealias  GraphQLErrorBuilder = GraphqlErrorBuilder<*>

/**
 * A helper class that can to deal with graphql errors
 */
// todo: make internal once we merge api/ and engine-nextgen
object ErrorUtil {
    fun createGraphQLErrorsFromRawErrors(errors: List<Map<String, Any?>>): List<GraphQLError> {
        return errors.map {
            createGraphQLErrorFromRawError(it)
        }
    }

    fun createGraphQLErrorFromRawError(rawError: Map<String, Any?>): GraphQLError {
        val errorBuilder = GraphQLErrorBuilder.newError()
        errorBuilder.message(rawError["message"].toString())
        errorBuilder.errorType(ErrorType.DataFetchingException)
        extractLocations(errorBuilder, rawError)
        extractPath(errorBuilder, rawError)
        extractExtensions(errorBuilder, rawError)
        return errorBuilder.build()
    }

    // It needs to be this. A class cast exception will tell it when it's not
    private fun extractPath(errorBuilder: GraphQLErrorBuilder, rawError: Map<String, Any?>) {
        val path = rawError["path"] as List<Any?>?
        if (path != null) {
            errorBuilder.path(path)
        }
    }

    // It needs to be this. A class cast exception will tell it when it's not
    private fun extractExtensions(errorBuilder: GraphQLErrorBuilder, rawError: Map<String, Any?>) {
        @Suppress("UNCHECKED_CAST")
        val extensions = rawError["extensions"] as Map<String?, Any?>?
        if (extensions != null) {
            errorBuilder.extensions(extensions)
        }
    }

    // It needs to be this. A class cast exception will tell it when it's not
    private fun extractLocations(errorBuilder: GraphQLErrorBuilder, rawError: Map<String, Any?>) {
        val locations = rawError["locations"] as MutableList<*>?
        if (locations != null) {
            val sourceLocations = ArrayList<SourceLocation?>()

            for (locationObj in locations) {
                @Suppress("UNCHECKED_CAST")
                val location = locationObj as Map<String?, Any?>?
                if (location != null) {
                    val line = location["line"] as Int?
                    val column = location["column"] as Int?
                    if (line != null && column != null) {
                        sourceLocations.add(SourceLocation(line, column))
                    }
                }
            }

            errorBuilder.locations(sourceLocations)
        }
    }
}
