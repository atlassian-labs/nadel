package graphql.nadel.tests.next

import com.fasterxml.jackson.module.kotlin.readValue
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.tests.jsonObjectMapper
import org.intellij.lang.annotations.Language

abstract class TestSnapshot {
    abstract val calls: List<ExpectedServiceCall>
    abstract val response: ExpectedNadelResponse
}

data class ExpectedServiceCall(
    val service: String,
    @Language("GraphQL")
    val query: String,
    @Language("JSON")
    val variables: JsonMap,
    @Language("JSON")
    val response: JsonMap,
    val delayedResponses: List<JsonMap>,
) {
    constructor(
        service: String,
        @Language("GraphQL")
        query: String,
        @Language("JSON")
        variables: String,
        @Language("JSON")
        response: String,
        delayedResponses: List<String>,
    ) : this(
        service = service,
        query = query,
        variables = jsonObjectMapper.readValue(variables),
        response = jsonObjectMapper.readValue(response),
        delayedResponses = delayedResponses.map<String, JsonMap>(jsonObjectMapper::readValue),
    )
}

data class ExpectedNadelResponse(
    @Language("JSON")
    val response: String,
    val delayedResponses: List<String>,
)
