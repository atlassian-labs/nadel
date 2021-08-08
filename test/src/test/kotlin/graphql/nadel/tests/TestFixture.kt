package graphql.nadel.tests

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import graphql.language.AstSorter
import graphql.language.Document
import graphql.nadel.NadelEngine
import graphql.nadel.NadelExecutionEngineFactory
import graphql.nadel.NextgenEngine
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.tests.transforms.RemoveFieldTestTransform
import graphql.parser.Parser
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties

data class TestFixture(
    val name: String,
    val enabled: EngineTypeEnabled = EngineTypeEnabled(),
    val ignored: EngineTypeIgnored = EngineTypeIgnored(),
    val overallSchema: Map<String, String>,
    val underlyingSchema: Map<String, String>,
    val query: String,
    val variables: Map<String, Any?>,
    val serviceCalls: ServiceCalls,
    @JsonProperty("response")
    val responseJsonString: String?,
    val exception: ExpectedException?,
    val deferredResponses: List<DeferredResponse>?
) {
    @get:JsonIgnore
    val response: JsonMap? by lazy {
        responseJsonString?.let(jsonObjectMapper::readValue)
    }
}

val customTestTransforms = listOf(
    RemoveFieldTestTransform(),
)

data class EngineTypeFactories(
    override val current: NadelExecutionEngineFactory = NadelExecutionEngineFactory(::NadelEngine),
    // TODO: test transforms should be set via the engine test hook.
    override val nextgen: NadelExecutionEngineFactory = NadelExecutionEngineFactory { nadel ->
        NextgenEngine(nadel, customTestTransforms)
    },
) : NadelEngineTypeValueProvider<NadelExecutionEngineFactory> {
    val all = engines(factories = this)

    companion object {
        private fun engines(factories: EngineTypeFactories): List<Pair<NadelEngineType, NadelExecutionEngineFactory>> {
            return EngineTypeFactories::class.memberProperties
                .filter {
                    it.returnType == NadelExecutionEngineFactory::class.createType()
                }
                .map {
                    NadelEngineType.valueOf(it.name) to it.get(factories) as NadelExecutionEngineFactory
                }
        }
    }
}

data class EngineTypeEnabled(
    override val current: Boolean = true,
    override val nextgen: Boolean = false,
) : NadelEngineTypeValueProvider<Boolean>

data class EngineTypeIgnored(
    override val current: Boolean = false,
    override val nextgen: Boolean = false,
) : NadelEngineTypeValueProvider<Boolean>

data class ServiceCalls(
    override val current: List<ServiceCall> = emptyList(),
    override val nextgen: List<ServiceCall> = emptyList(),
) : NadelEngineTypeValueProvider<List<ServiceCall>>

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

data class DeferredResponse(
    val path: List<String>?,
    val label: String?,
    @JsonProperty("data")
    val dataString: String?,
    val hasNext: Boolean
) {

    @get:JsonIgnore
    val data: JsonMap? by lazy {
        x()
    }
    
    fun x (): JsonMap? {
        return if(dataString != null) {
            jsonObjectMapper.readValue(dataString)
        } else {
            null
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
