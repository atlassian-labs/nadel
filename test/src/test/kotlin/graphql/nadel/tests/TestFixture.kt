package graphql.nadel.tests

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import graphql.language.AstSorter
import graphql.language.Document
import graphql.nadel.Nadel
import graphql.nadel.NadelEngine
import graphql.nadel.NadelExecutionEngine
import graphql.nadel.NextgenEngine
import graphql.nadel.enginekt.util.JsonMap
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
    val operationName: String? = null,
    val serviceCalls: ServiceCalls,
    @JsonProperty("response")
    val responseJsonString: String?,
    val exception: ExpectedException?,
) {
    @get:JsonIgnore
    val response: JsonMap? by lazy {
        responseJsonString?.let(jsonObjectMapper::readValue)
    }
}

data class EngineTypeFactories(
    override val current: TestEngineFactory = TestEngineFactory { nadel, _ ->
        NadelEngine(nadel)
    },
    override val nextgen: TestEngineFactory = TestEngineFactory { nadel, testHook ->
        NextgenEngine(
            nadel = nadel,
            transforms = testHook.customTransforms,
        )
    },
) : NadelEngineTypeValueProvider<TestEngineFactory> {
    val all = engines(factories = this)

    companion object {
        private fun engines(factories: EngineTypeFactories): List<Pair<NadelEngineType, TestEngineFactory>> {
            return EngineTypeFactories::class.memberProperties
                .filter {
                    it.returnType == TestEngineFactory::class.createType()
                }
                .map {
                    NadelEngineType.valueOf(it.name) to it.get(factories) as TestEngineFactory
                }
        }
    }
}

fun interface TestEngineFactory {
    fun make(nadel: Nadel, testHook: EngineTestHook): NadelExecutionEngine
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
