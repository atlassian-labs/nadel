package graphql.nadel.tests

import com.fasterxml.jackson.annotation.JsonIgnore
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
import strikt.assertions.isA
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
        .onEach {
            println("Loading ${it.nameWithoutExtension}")
        }
        .map(File::readText)
        .map<String, TestFixture>(yamlObjectMapper::readValue)
        .forEach { fixture ->
            engineFactories.all
                .asSequence()
                .filter { (engine) ->
                    fixture.enabled.get(engine = engine)
                }
                .forEach { (engine, engineFactory) ->
                    val execute: suspend TestContext.() -> Unit = {
                        execute(fixture, engine, engineFactory)
                    }
                    if (fixture.ignored.get(engine = engine)) {
                        xtest("$engine ${fixture.name}", execute)
                    } else {
                        test("$engine ${fixture.name}", execute)
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
                        val incomingQuery = params.query
                        val incomingQueryPrinted = AstPrinter.printAst(
                            astSorter.sort(incomingQuery),
                        )
                        println(incomingQueryPrinted)

                        val response = fixture.serviceCalls[engine]
                            .filter { call ->
                                call.serviceName == serviceName
                            }
                            .singleOrNull {
                                AstPrinter.printAst(it.request.document) == incomingQueryPrinted
                            }?.response
                            ?: fail("Unable to match service call")

                        @Suppress("UNCHECKED_CAST")
                        CompletableFuture.completedFuture(
                            ServiceExecutionResult(
                                response["data"] as JsonMap?,
                                response["errors"] as List<JsonMap>? ?: mutableListOf(),
                                response["extensions"] as JsonMap? ?: mutableMapOf(),
                            ),
                        )
                    } catch (e: Throwable) {
                        fail("Unable to invoke service", e)
                    }
                }
            }

            override fun getUnderlyingTypeDefinitions(serviceName: String): TypeDefinitionRegistry {
                return SchemaParser().parse(fixture.underlyingSchema[serviceName])
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
    assertJsonObject(
        subject = response.toSpecification().let {
            mapOf(
                "data" to it["data"],
                "errors" to (it["errors"] ?: emptyList<JsonMap>()),
                // "extensions" to (it["extensions"] ?: emptyMap<String, Any?>()),
            ).also {
                println("Overall response")
                println(it)
                println()
            }
        },
        expected = fixture.response.let {
            mapOf(
                "data" to it["data"],
                "errors" to (it["errors"] ?: emptyList<JsonMap>()),
                // "extensions" to (it["extensions"] ?: emptyMap<String, Any?>()),
            ).also {
                println("Expecting response")
                println(it)
                println()
            }
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

fun assertJsonObject(subject: JsonMap, expected: JsonMap) {
    return expectThat(subject) {
        assertJsonObject(expectedMap = expected)
    }
}

fun Assertion.Builder<JsonMap>.assertJsonObject(expectedMap: JsonMap) {
    keysEqual(expectedMap.keys)

    assert("keys are all strings") { subject ->
        @Suppress("USELESS_CAST") // We're checking if the erased type holds up
        for (key in (subject.keys as Set<Any>)) {
            if (key !is String) {
                fail(description = "%s is not a string", actual = key)
                return@assert
            }
        }
        pass()
    }

    compose("contents match expected") { subjectMap ->
        subjectMap.entries.forEach { (key, subjectValue) ->
            assertJsonEntry(key, subjectValue, expectedValue = expectedMap[key])
        }
    } then {
        if (allPassed || failedCount == 0) pass() else fail()
    }
}

fun Assertion.Builder<JsonMap>.assertJsonEntry(key: String, subjectValue: Any?, expectedValue: Any?) {
    get("""entry "$key"""") { subjectValue }
        .assertJsonValue(subjectValue, expectedValue)
}

fun <T> Assertion.Builder<T>.assertJsonValue(subjectValue: Any?, expectedValue: Any?) {
    when (subjectValue) {
        is AnyMap -> {
            assert("is same type as expected value") {
                if (expectedValue is AnyMap) {
                    pass()
                    @Suppress("UNCHECKED_CAST")
                    isA<JsonMap>().assertJsonObject(expectedMap = expectedValue as JsonMap)
                } else {
                    fail("did not expect JSON object")
                }
            }
        }
        is AnyList -> {
            assert("is same type as expected value") {
                if (expectedValue is AnyList) {
                    pass()
                    @Suppress("UNCHECKED_CAST")
                    isA<List<Any>>().assertJsonArray(expectedValue = expectedValue as List<Any>)
                } else {
                    fail("did not expect JSON array")
                }
            }
        }
        else -> {
            assert("equals expected value") {
                if (subjectValue == expectedValue) {
                    pass()
                } else {
                    fail("""expected "$expectedValue" but got "$subjectValue"""")
                }
            }
        }
    }
}

private fun <T> Assertion.Builder<List<T>>.assertJsonArray(expectedValue: List<T>) {
    compose("all elements match expected:") { subject ->
        subject.forEachIndexed { index, element ->
            get("element $index") { element }
                .assertJsonValue(subjectValue = element, expectedValue[index])
        }
    } then {
        if (allPassed) pass() else fail()
    }
    assert("size matches expected") { subject ->
        if (subject.size == expectedValue.size) {
            pass()
        } else {
            fail("expected size ${expectedValue.size} but got ${subject.size}")
        }
    }
}

private data class TestFixture(
    val name: String,
    val enabled: EngineEnabled = EngineEnabled(),
    val ignored: EngineIgnored = EngineIgnored(),
    val overallSchema: Map<String, String>,
    val underlyingSchema: Map<String, String>,
    val query: String,
    val variables: Map<String, Any?>,
    val serviceCalls: ServiceCalls,
    @JsonProperty("response")
    val responseJsonString: String?,
    val exception: ExpectedException?,
) {
    @get:JsonIgnore
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

data class ServiceCalls(
    override val current: List<ServiceCall> = emptyList(),
    override val nextgen: List<ServiceCall> = emptyList(),
) : Engines<List<ServiceCall>>

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
            private val documentParser = DocumentParser()
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

private val NON_LATIN: Pattern = Pattern.compile("[^\\w-]")
private val WHITESPACE: Pattern = Pattern.compile("[\\s]")

fun String.toSlug(): String {
    val noWhitespace = WHITESPACE.matcher(this).replaceAll("-")
    val normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD)
    val slug = NON_LATIN.matcher(normalized).replaceAll("")
    return slug.toLowerCase(Locale.ENGLISH)
}
