package graphql.nadel.tests

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import graphql.language.AstPrinter
import graphql.language.AstSorter
import graphql.language.Document
import graphql.nadel.Nadel
import graphql.nadel.NadelEngine
import graphql.nadel.NadelExecutionEngineFactory
import graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import graphql.nadel.NextgenEngine
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.util.AnyList
import graphql.nadel.enginekt.util.AnyMap
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.tests.util.fixtures.EngineTestHook
import graphql.nadel.tests.util.getPropertyValue
import graphql.nadel.tests.util.keysEqual
import graphql.nadel.tests.util.packageName
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestContext
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.fail
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.io.File
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties
import graphql.parser.Parser as DocumentParser

private val jsonObjectMapper = ObjectMapper().findAndRegisterModules()

private val yamlObjectMapper = YAMLFactory().let(::ObjectMapper).findAndRegisterModules()

class EngineTests : FunSpec({
    val engineFactories = EngineFactories()
    val fixturesDir = File(javaClass.classLoader.getResource("fixtures")!!.path)
    fixturesDir.listFiles()!!
        .asSequence()
        .map(File::readText)
        .map<String, TestFixture>(yamlObjectMapper::readValue)
        .forEach { fixture ->
            engineFactories.all
                .asSequence()
                // TODO: remove
                // .filter { (key) -> key == Engine.nextgen }
                .filter { (key) ->
                    getPropertyValue(instance = fixture.enabled, propertyName = key.name)
                }
                .forEach { (key, engineFactory) ->
                    val execute: suspend TestContext.() -> Unit = {
                        execute(fixture, key, engineFactory)
                    }
                    if (getPropertyValue(instance = fixture.ignored, propertyName = key.name)) {
                        xtest("$key ${fixture.name}", execute)
                    } else {
                        test("$key ${fixture.name}", execute)
                    }
                }
        }
})

private suspend fun execute(
    fixture: TestFixture,
    engine: Engine,
    engineFactory: NadelExecutionEngineFactory,
) {
    val testHooks = getTestHook(fixture)

    val nadel = Nadel.newNadel()
        .engineFactory(engineFactory)
        .dsl(fixture.overallSchema)
        .serviceExecutionFactory(object : ServiceExecutionFactory {
            private val astSorter = AstSorter()

            override fun getServiceExecution(serviceName: String): ServiceExecution {
                return ServiceExecution { params ->
                    try {
                        val incomingQueryPrinted = AstPrinter.printAst(
                            astSorter.sort(
                                params.query,
                            ),
                        )
                        println(incomingQueryPrinted)

                        val call = fixture.calls[engine].singleOrNull {
                            AstPrinter.printAst(it.request.document) == incomingQueryPrinted
                        } ?: Unit.let { // Creates code block for null
                            fail("Unable to match service call")
                        }

                        @Suppress("UNCHECKED_CAST")
                        CompletableFuture.completedFuture(
                            ServiceExecutionResult(
                                call.response["data"] as JsonMap?,
                                call.response["errors"] as List<JsonMap>?,
                                call.response["extensions"] as JsonMap?,
                            ),
                        )
                    } catch (e: Throwable) {
                        fail("Unable to invoke service", e)
                    }
                }
            }

            override fun getUnderlyingTypeDefinitions(serviceName: String): TypeDefinitionRegistry {
                return SchemaParser().parse(fixture.services[serviceName])
            }
        })
        .let {
            testHooks?.makeNadel(engine, it) ?: it
        }
        .build()

    val response = try {
        nadel.execute(newNadelExecutionInput()
            .query(fixture.query)
            .variables(fixture.variables)
            .artificialFieldsUUID("UUID")
            .build())
            .await()
    } catch (e: Exception) {
        if (fixture.exception != null) {
            if (fixture.exception.message.matches(e.message ?: "")) {
                // Pass test
                return
            }
        }
        fail("Unexpected error during engine execution", e)
    }

    if (fixture.exception != null) {
        fail("Expected exception did not occur")
    }

    // TODO: check extensions one day - right now they don't match up as dumped tests weren't fully E2E but tests are
    assertJsonTree(
        expected = fixture.response.let {
            mapOf(
                "data" to it["data"],
                "errors" to (it["errors"] ?: emptyList<JsonMap>()),
                // "extensions" to (it["extensions"] ?: emptyMap<String, Any?>()),
            )
        },
        actual = response.toSpecification().let {
            mapOf(
                "data" to it["data"],
                "errors" to (it["errors"] ?: emptyList<JsonMap>()),
                // "extensions" to (it["extensions"] ?: emptyMap<String, Any?>()),
            )
        },
    )
}

private fun getTestHook(fixture: TestFixture): EngineTestHook? {
    val hookClass = try {
        Class.forName(
            sequenceOf(
                EngineTestHook::class.java.packageName,
                "hooks",
                fixture.name.toSlug(),
            ).joinToString(separator = "."),
        )
    } catch (_: ClassNotFoundException) {
        println("No hook class found")
        return null
    }
    return hookClass.newInstance() as EngineTestHook
}

fun assertJsonTree(expected: JsonMap, actual: JsonMap) {
    return expectThat(actual) {
        assertJsonTree(expected = expected)
    }
}

fun Assertion.Builder<JsonMap>.assertJsonTree(
    expected: JsonMap,
) {
    keysEqual(expected.keys)

    get { entries }.all {
        // JSON map needs String keys
        get { key }.isA<String>()

        compose("value") { entry ->
            when (val expectedValue = expected[entry.key]) {
                is AnyMap -> get { value }.isA<JsonMap>().and {
                    @Suppress("UNCHECKED_CAST")
                    this@and.assertJsonTree(expectedValue as JsonMap)
                }
                is AnyList -> get { value }.and { }
                is Number -> get { value }.isEqualTo(expectedValue)
                is String -> get { value }.isEqualTo(expectedValue)
                is Boolean -> get { value }.isEqualTo(expectedValue)
                null -> get { value }.isNull()
                else -> error("Unknown type ${expectedValue.javaClass}")
            }
        } then {
            if (allPassed || failedCount == 0) pass() else fail()
        }
    }
}

private data class TestFixture(
    val name: String,
    val enabled: EngineEnabled = EngineEnabled(),
    val ignored: EngineIgnored = EngineIgnored(),
    val overallSchema: Map<String, String>,
    val services: Map<String, String>,
    val query: String,
    val variables: Map<String, Any?>,
    val calls: EngineCalls,
    @JsonProperty("response")
    private val responseJsonString: String?,
    val exception: ExecutionException?,
) {
    val response: Map<String, Any?> by lazy {
        if (responseJsonString != null) {
            jsonObjectMapper.readValue(responseJsonString)
        } else {
            emptyMap()
        }
    }
}

enum class Engine {
    current,
    nextgen,
}

interface Engines<T> {
    val current: T
    val nextgen: T

    operator fun get(engine: Engine): T {
        return when (engine) {
            Engine.current -> current
            Engine.nextgen -> nextgen
        }
    }
}

data class EngineFactories(
    override val current: NadelExecutionEngineFactory = NadelExecutionEngineFactory(::NadelEngine),
    override val nextgen: NadelExecutionEngineFactory = NadelExecutionEngineFactory(::NextgenEngine),
) : Engines<NadelExecutionEngineFactory> {
    val all = engines(factories = this)

    companion object {
        private fun engines(factories: EngineFactories): List<Pair<Engine, NadelExecutionEngineFactory>> {
            return EngineFactories::class.memberProperties
                .filter {
                    it.returnType == NadelExecutionEngineFactory::class.createType()
                }
                .map {
                    Engine.valueOf(it.name) to it.get(factories) as NadelExecutionEngineFactory
                }
        }
    }
}

data class EngineEnabled(
    override val current: Boolean = true,
    override val nextgen: Boolean = false,
) : Engines<Boolean>

data class EngineIgnored(
    override val current: Boolean = false,
    override val nextgen: Boolean = false,
) : Engines<Boolean>

data class EngineCalls(
    override val current: List<ServiceCall> = emptyList(),
    override val nextgen: List<ServiceCall> = emptyList(),
) : Engines<List<ServiceCall>>

data class ServiceCall(
    val request: Request,
    @JsonProperty("response")
    private val responseJsonString: String,
) {
    val response: JsonMap by lazy {
        jsonObjectMapper.readValue(responseJsonString)
    }

    data class Request(
        val query: String,
        val variables: Map<String, Any?> = emptyMap(),
        val operationName: String? = null,
    ) {
        val document: Document by lazy {
            astSorter.sort(
                documentParser.parseDocument(query)
            )
        }

        companion object {
            private val astSorter = AstSorter()
            private val documentParser = DocumentParser()
        }
    }
}

data class ExecutionException(
    @JsonProperty("message")
    private val messageString: String = "",
    private val ignoreMessageCase: Boolean = false,
) {
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

private val NON_LATIN: Pattern = Pattern.compile("[^\\w-]")
private val WHITESPACE: Pattern = Pattern.compile("[\\s]")

fun String.toSlug(): String {
    val noWhitespace = WHITESPACE.matcher(this).replaceAll("-")
    val normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD)
    val slug = NON_LATIN.matcher(normalized).replaceAll("")
    return slug.toLowerCase(Locale.ENGLISH)
}


