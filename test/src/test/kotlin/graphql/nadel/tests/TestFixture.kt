package graphql.nadel.tests

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import graphql.language.AstSorter
import graphql.language.Document
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionEngine
import graphql.nadel.enginekt.util.JsonMap
import graphql.parser.Parser

data class TestFixture(
    val name: String,
    val enabled: Boolean,
    val ignored: Boolean,
    val overallSchema: Map<String, String>,
    val underlyingSchema: Map<String, String>,
    val query: String,
    val variables: Map<String, Any?>,
    val operationName: String? = null,
    val serviceCalls: List<ServiceCall>,
    @JsonProperty("response")
    val responseJsonString: String?,
    val exception: ExpectedException?,
) {
    @get:JsonIgnore
    val response: JsonMap? by lazy {
        responseJsonString?.let(jsonObjectMapper::readValue)
    }
}

fun interface TestEngineFactory {
    fun make(nadel: Nadel, testHook: EngineTestHook): NadelExecutionEngine
}

data class ServiceCall(
    val serviceName: String,
    val request: Request,
    @JsonProperty("response")
    val responseJsonString: String,
) {
    @get:JsonIgnore
    val response: JsonMap by lazy {
        jsonObjectMapper.readValue(responseJsonString)
    }

    data class Request(
        val query: String,
        val variables: Map<String, Any?> = emptyMap(),
        val operationName: String? = null,
    ) {
        @get:JsonIgnore
        val document: Document by lazy {
            astSorter.sort(
                documentParser.parseDocument(query)
            )
        }

        companion object {
            private val astSorter = AstSorter()
            private val documentParser = Parser()
        }
    }
}

data class ExpectedException(
    @JsonProperty("message")
    val messageString: String = "",
    val ignoreMessageCase: Boolean = false,
) {
    @get:JsonIgnore
    val message: Regex by lazy {
        Regex(
            messageString,
            setOfNotNull(
                when (ignoreMessageCase) {
                    true -> RegexOption.IGNORE_CASE
                    else -> null
                },
            ),
        )
    }
}
