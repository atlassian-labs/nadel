package graphql.nadel.tests

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.readValue
import graphql.language.AstSorter
import graphql.language.Document
import graphql.nadel.engine.util.JsonMap
import graphql.parser.Parser

/**
 * Each test YAML file is parsed into this TextFixture class.  Hence, the properties specified here are the properties
 * allowable in the test YAML.
 */
data class TestFixture(
    val name: String,
    val enabled: Boolean,
    @JsonInclude(NON_DEFAULT)
    val ignored: Boolean,
    val overallSchema: Map<String, String>,
    val underlyingSchema: Map<String, String>,
    val query: String,
    val variables: Map<String, Any?>,
    @JsonInclude(NON_NULL)
    val operationName: String? = null,
    val serviceCalls: List<ServiceCall>,
    @JsonProperty("response")
    val responseJsonString: String?,
    @JsonProperty("incrementalResponse")
    val incrementalResponseJsonString: IncrementalResponse?,
    @JsonInclude(NON_NULL)
    val exception: ExpectedException?,
) {
    @get:JsonIgnore
    val response: JsonMap? by lazy {
        responseJsonString?.let(jsonObjectMapper::readValue)
    }
}

data class ServiceCall(
    val serviceName: String,
    val request: Request,
    @JsonProperty("response")
    val responseJsonString: String?,
    val incrementalResponse: IncrementalResponse?,
) {
    @get:JsonIgnore
    val response: JsonMap? by lazy {
        responseJsonString?.let {
            jsonObjectMapper.readValue(it)
        }
    }

    data class Request(
        val query: String,
        val variables: Map<String, Any?> = emptyMap(),
        @JsonInclude(NON_NULL)
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
    @JsonInclude(NON_DEFAULT)
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

data class IncrementalResponse(
    @JsonProperty("initialResponse")
    val initialResponseJsonString: String,
    @JsonProperty("delayedResponses")
    val delayedResponsesJsonString: String,
) {
    @get:JsonIgnore
    val initialResponse: JsonMap by lazy {
        jsonObjectMapper.readValue(initialResponseJsonString)
    }

    @get:JsonIgnore
    val delayedResponses: List<JsonMap> by lazy {
        jsonObjectMapper.readValue(delayedResponsesJsonString, object : TypeReference<List<JsonMap>>() {})
    }
}