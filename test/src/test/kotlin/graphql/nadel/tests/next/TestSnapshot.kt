package graphql.nadel.tests.next

import com.fasterxml.jackson.module.kotlin.readValue
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.tests.jsonObjectMapper
import org.intellij.lang.annotations.Language

abstract class TestSnapshot {
    abstract val calls: List<ExpectedServiceCall>
    abstract val result: ExpectedNadelResult
}

data class ExpectedServiceCall(
    val service: String,
    @Language("GraphQL")
    val query: String,
    @Language("JSON")
    val variables: JsonMap,
    @Language("JSON")
    val result: JsonMap,
    val delayedResults: List<JsonMap>,
) {
    constructor(
        service: String,
        @Language("GraphQL")
        query: String,
        @Language("JSON")
        variables: String,
        @Language("JSON")
        result: String,
        delayedResults: List<String>,
    ) : this(
        service = service,
        query = query,
        variables = jsonObjectMapper.readValue(variables),
        result = jsonObjectMapper.readValue(result),
        delayedResults = delayedResults.map<String, JsonMap>(jsonObjectMapper::readValue),
    )

    override fun hashCode(): Int {
        // Comparing the Maps is not accurate
        throw UnsupportedOperationException("Avoid using")
    }

    override fun equals(other: Any?): Boolean {
        // Comparing the Maps is not accurate
        throw UnsupportedOperationException("Avoid using")
    }
}

data class ExpectedNadelResult(
    @Language("JSON")
    val result: String,
    val delayedResults: List<JsonMap>,
) {
    companion object {
        operator fun invoke(
            @Language("JSON")
            result: String,
            delayedResults: List<String>,
        ): ExpectedNadelResult {
            return ExpectedNadelResult(
                result = result,
                delayedResults = delayedResults.map(jsonObjectMapper::readValue),
            )
        }
    }

    override fun hashCode(): Int {
        // Comparing the Maps is not accurate
        throw UnsupportedOperationException("Avoid using")
    }

    override fun equals(other: Any?): Boolean {
        // Comparing the Maps is not accurate
        throw UnsupportedOperationException("Avoid using")
    }
}
