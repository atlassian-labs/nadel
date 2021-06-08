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
import graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.util.AnyList
import graphql.nadel.enginekt.util.AnyMap
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.tests.util.keysEqual
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
import java.util.concurrent.CompletableFuture
import graphql.parser.Parser as DocumentParser

private val jsonObjectMapper = ObjectMapper().findAndRegisterModules()

private val yamlObjectMapper = YAMLFactory().let(::ObjectMapper).findAndRegisterModules()

class EngineTests : FunSpec({
    val fixturesDir = File(javaClass.classLoader.getResource("fixtures")!!.path)
    fixturesDir.listFiles()!!
        .asSequence()
        .map(File::readText)
        .map<String, TestFixture>(yamlObjectMapper::readValue)
        .forEach { fixture ->
            val execute: suspend TestContext.() -> Unit = {
                execute(fixture)
            }
            if (fixture.ignored) {
                xtest(fixture.name, execute)
            } else {
                test(fixture.name, execute)
            }
        }
})

private suspend fun execute(fixture: TestFixture) {
    val nadel = Nadel.newNadel()
        .engineFactory(::NadelEngine)
        .dsl(fixture.overallSchema)
        .serviceExecutionFactory(object : ServiceExecutionFactory {
            private val astSorter = AstSorter()

            override fun getServiceExecution(serviceName: String): ServiceExecution {
                return ServiceExecution { params ->
                    try {
                        val incomingQuery = AstPrinter.printAst(
                            astSorter.sort(
                                params.query,
                            ),
                        )
                        val call = fixture.calls.singleOrNull {
                            AstPrinter.printAst(it.request.document) == incomingQuery
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
    val ignored: Boolean = false,
    val overallSchema: Map<String, String>,
    val services: Map<String, String>,
    val query: String,
    val variables: Map<String, Any?>,
    val calls: List<ServiceCall>,
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
        val variables: Map<String, Any?>,
        val operationName: String,
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