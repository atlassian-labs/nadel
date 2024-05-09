package graphql.nadel.tests

import com.fasterxml.jackson.module.kotlin.readValue
import graphql.GraphQLError
import graphql.incremental.DeferPayload
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.incremental.DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult
import graphql.language.AstPrinter
import graphql.language.AstSorter
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionHints
import graphql.nadel.NadelExecutionInput.Companion.newNadelExecutionInput
import graphql.nadel.NadelIncrementalServiceExecutionResult
import graphql.nadel.NadelSchemas
import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.AnyMap
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.tests.util.getAncestorFile
import graphql.nadel.tests.util.requireIsDirectory
import graphql.nadel.tests.util.toSlug
import graphql.nadel.validation.NadelSchemaValidation
import graphql.nadel.validation.NadelSchemaValidationError
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestContext
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.asPublisher
import org.junit.jupiter.api.fail
import org.reactivestreams.Publisher
import java.io.File
import java.math.BigInteger
import java.util.concurrent.CompletableFuture

/**
 * Enter in the name of the test here, leave blank to run all tests.
 *
 * Name can be any of:
 * 1. The test file name e.g. hydration-inside-a-renamed-field.yml
 * 2. Test name e.g. hydration inside a renamed field
 * 3. Copy paste output from selecting a test in the IntelliJ e.g. java:test://graphql.nadel.tests.EngineTests.current hydration inside a renamed field
 */
private val singleTestToRun = (System.getenv("TEST_NAME") ?: "")
    .removePrefix("java:test://graphql.nadel.tests.EngineTests.current")
    .removePrefix("java:test://graphql.nadel.tests.EngineTests.nextgen")
    .removeSuffix(".yml")
    .removeSuffix(".yaml")
    .trim()

/**
 * Define a default state for the hints, so all tests will execute using that state.
 *
 * If you need to override some of the hints here (or define new ones), use the
 * NadelExecutionInput.Builder#transformExecutionHints method in your test hook class.
 */
private val defaultHints = NadelExecutionHints.newHints()
    .build()

private val sep = "-".repeat(50)

class EngineTests : FunSpec({
    println(
        """
        $sep
        
        In order to run a specific test set TEST_NAME=xxxxx before running the EngineTests
        
        The name will be repeated per test
        
        $sep
        """.trimIndent(),
    )

    val fixturesDir = File(javaClass.classLoader.getResource("fixtures")!!.path)
        // Note: resources end up in nadel/test/build/resources/test/fixtures/
        .getAncestorFile("build").parentFile
        .requireIsDirectory()
        .let { moduleRootDir ->
            File(moduleRootDir, "src/test/resources/fixtures/").requireIsDirectory()
        }

    fixturesDir.walkTopDown()
        .asSequence()
        .filter {
            it.extension == "yml" || it.extension == "yaml"
        }
        .onEach {
            println("Loading ${it.nameWithoutExtension}")
        }
        .map { file ->
            file
                .let(File::readText)
                .let<String, TestFixture>(yamlObjectMapper::readValue)
                .also { fixture ->
                    // Rename fixtures if they have the wrong name
                    val expectedName = fixture.name.toSlug()
                    if (file.nameWithoutExtension != expectedName) {
                        val expectedFile = File(file.parentFile, "$expectedName.${file.extension}")
                        if (expectedFile.exists()) {
                            fail("${file.path} should be named ${expectedFile.path} but the latter already exists")
                        }
                        file.renameTo(expectedFile)
                    }
                }
        }
        .filter {
            it.enabled
        }
        .filter { fixture ->
            if (singleTestToRun.isBlank()) {
                true
            } else {
                fixture.name.equals(singleTestToRun, ignoreCase = true)
                    || fixture.name.toSlug().equals(singleTestToRun, ignoreCase = true)
            }
        }
        .forEach { fixture ->
            println(fixture.name)

            // Run for tests that don't have nextgen calls
            val execute: suspend TestContext.() -> Unit = {
                execute(fixture = fixture)
            }
            if (fixture.ignored) {
                xtest(fixture.name, execute)
            } else {
                test(fixture.name, execute)
            }
        }
})

private suspend fun execute(
    fixture: TestFixture,
    testHook: EngineTestHook = getTestHook(fixture) ?: EngineTestHook.noOp,
) {
    val printLock = Any()
    fun printSyncLine(message: String): Unit = synchronized(printLock) {
        println(message)
    }

    fun printSyncLine(message: Any?): Unit = synchronized(printLock) {
        println(message)
    }

    fun printSyncLine(): Unit = synchronized(printLock) {
        println()
    }

    printSyncLine("\n$sep\n${fixture.name}\n$sep")

    try {
        validate(fixture, testHook)

        val nadel: Nadel = Nadel.newNadel()
            .schemaTransformationHook(testHook.schemaTransformationHook)
            .transforms(testHook.customTransforms)
            .overallSchemas(fixture.overallSchema)
            .underlyingSchemas(fixture.underlyingSchema)
            .overallWiringFactory(testHook.wiringFactory)
            .underlyingWiringFactory(testHook.wiringFactory)
            .serviceExecutionFactory(object : ServiceExecutionFactory {
                private val astSorter = AstSorter()
                private val serviceCalls = fixture.serviceCalls.toMutableList()

                override fun getServiceExecution(serviceName: String): ServiceExecution {
                    val serviceExecution = ServiceExecution { params ->
                        try {
                            val incomingQuery = params.query
                            val actualVariables = fixVariables(params.variables)
                            val actualOperationName = params.operationDefinition.name
                            val actualQuery = AstPrinter.printAst(
                                astSorter.sort(incomingQuery),
                            )
                            printSyncLine(actualQuery)

                            fun failWithFixtureContext(message: String): Nothing {
                                fail(
                                    """${message}
                                        |   fixture : '${fixture.name}' 
                                        |   service : '${serviceName}' 
                                        |   query : '${actualQuery}' 
                                        |   variables : '${actualVariables}' 
                                        |   operation : '${actualOperationName}' 
                                        """.trimMargin()
                                )
                            }

                            synchronized(serviceCalls) {
                                val indexOfCall = serviceCalls
                                    .indexOfFirst {
                                        it.serviceName == serviceName
                                            && AstPrinter.printAst(it.request.document) == actualQuery
                                            && it.request.operationName == actualOperationName
                                            && it.request.variables == actualVariables
                                    }
                                    .takeIf { it != -1 }

                                if (indexOfCall != null) {
                                    val serviceCall = serviceCalls.removeAt(indexOfCall)
                                    if (serviceCall.incrementalResponse != null && serviceCall.response != null) {
                                        failWithFixtureContext("Cannot have both an incremental and non-incremental response")
                                    } else if (serviceCall.incrementalResponse != null) {
                                        transformIncrementalExecutionResult(serviceCall)
                                    } else if (serviceCall.response != null) {
                                        transformExecutionResult(serviceCall.response!!)
                                    } else {
                                        failWithFixtureContext("Service call had no response")
                                    }
                                } else {
                                    failWithFixtureContext("Unable to match service call")
                                }
                            }
                        } catch (e: Throwable) {
                            fail("Unable to invoke service '$serviceName'", e)
                        }
                    }
                    return testHook.wrapServiceExecution(serviceExecution)
                }

                private fun transformIncrementalExecutionResult(serviceCall: ServiceCall): CompletableFuture<ServiceExecutionResult> {
                    val incrementalItemPublisher: Publisher<DelayedIncrementalPartialResult> =
                        flowOf(*serviceCall.incrementalResponse!!.delayedResponses.toTypedArray()).map {
                            transformDelayedIncrementalPartialResult(it)
                        }.asPublisher()
                    val initialResponse = serviceCall.incrementalResponse.initialResponse

                    if ("incremental" in initialResponse) {
                        throw UnsupportedOperationException("We don't support this here")
                    }

                    return CompletableFuture.completedFuture(
                        NadelIncrementalServiceExecutionResult(
                            data = initialResponse["data"] as MutableJsonMap? ?: LinkedHashMap(),
                            errors = initialResponse["errors"] as MutableList<MutableJsonMap>? ?: ArrayList(),
                            extensions = initialResponse["extensions"] as MutableJsonMap? ?: LinkedHashMap(),
                            incremental = emptyList(), // todo: support this if we need it
                            incrementalItemPublisher = incrementalItemPublisher,
                            hasNext = true,
                        )
                    )
                }

                private fun transformExecutionResult(serviceCallResponse: JsonMap): CompletableFuture<ServiceExecutionResult> {
                    return CompletableFuture.completedFuture(
                        NadelServiceExecutionResultImpl(
                            serviceCallResponse["data"] as MutableJsonMap? ?: LinkedHashMap(),
                            serviceCallResponse["errors"] as MutableList<MutableJsonMap>? ?: ArrayList(),
                            serviceCallResponse["extensions"] as MutableJsonMap? ?: LinkedHashMap(),
                        ),
                    )
                }

                private fun transformDelayedIncrementalPartialResult(delayedResponse: JsonMap): DelayedIncrementalPartialResult {
                    val incrementalDataVal = delayedResponse["incremental"] as List<JsonMap>
                    return newIncrementalExecutionResult()
                        .hasNext(delayedResponse["hasNext"] as Boolean)
                        .apply {
                            if (delayedResponse["extensions"] != null) extensions(delayedResponse["extensions"] as Map<Any, Any>)
                        }
                        .incrementalItems(
                            incrementalDataVal.map {
                                DeferPayload.newDeferredItem()
                                    .data(it["data"])
                                    .path(it["path"] as List<Object>)
                                    .apply {
                                        if (it["label"] != null) it["label"] as String
                                        if (it["extensions"] != null) extensions(it["extensions"] as Map<Any, Any>)
                                        if (it["errors"] != null) errors(it["errors"] as List<GraphQLError>)
                                    }
                                    .build()
                            }
                        )
                        .build()
                }

                private fun fixVariables(variables: JsonMap): JsonMap {
                    return variables
                        .mapValues { (_, value) ->
                            fixVariableValue(value)
                        }
                }

                private fun fixVariables(variables: List<Any?>): List<Any?> {
                    return variables
                        .map {
                            fixVariableValue(it)
                        }
                }

                /**
                 * Fixes issues with test fixture having Int but where the engine produces BigInteger etc.
                 */
                private fun fixVariableValue(value: Any?): Any? {
                    return if (value is BigInteger) {
                        // Jackson will parse into the Int or Long depending on size
                        // Match that here
                        if (value.toLong() <= Int.MAX_VALUE) {
                            value.toInt()
                        } else {
                            value.toLong()
                        }
                    } else if (value is AnyMap) {
                        @Suppress("UNCHECKED_CAST")
                        fixVariables(value as JsonMap)
                    } else if (value is AnyList) {
                        fixVariables(value)
                    } else {
                        value
                    }
                }
            })
            .let {
                testHook.makeNadel(it)
            }
            .build()

        val response = nadel.execute(
            newNadelExecutionInput()
                .query(fixture.query)
                .variables(fixture.variables)
                .operationName(fixture.operationName)
                .let { builder ->
                    testHook.makeExecutionInput(
                        builder = builder.executionHints(
                            nadelExecutionHints = testHook
                                .makeExecutionHints(defaultHints.toBuilder())
                                .let {
                                    if (fixture.name.startsWith("new ")) {
                                        it.newBatchHydrationGrouping { true }
                                    } else {
                                        it
                                    }
                                }
                                .build(),
                        ),
                    )
                }
                .build(),
        ).await()

        if (fixture.exception != null) {
            fail("Expected exception did not occur")
        }

        val expectedResponse = fixture.response
        if (expectedResponse != null) {
            // TODO: check extensions one day - right now they don't match up as dumped tests weren't fully E2E but tests are
            assertJsonObject(
                subject = response.toSpecification().let {
                    mapOf(
                        "data" to it["data"],
                        "errors" to (it["errors"] ?: emptyList<JsonMap>()),
                        // "extensions" to (it["extensions"] ?: emptyMap<String, Any?>()),
                    ).also {
                        printSyncLine("Overall response")
                        printSyncLine(jsonObjectMapper.writeValueAsString(it))
                        printSyncLine()
                    }
                },
                expected = expectedResponse.let {
                    mapOf(
                        "data" to it["data"],
                        "errors" to (it["errors"] ?: emptyList<JsonMap>()),
                        // "extensions" to (it["extensions"] ?: emptyMap<String, Any?>()),
                    ).also {
                        printSyncLine("Expecting response")
                        printSyncLine(jsonObjectMapper.writeValueAsString(it))
                        printSyncLine()
                    }
                },
            )
        }

        testHook.assertResult(response)
    } catch (e: Throwable) {
        if (fixture.exception?.message?.matches(e.message ?: "") == true) {
            return
        }
        if (testHook.assertFailure(e)) {
            return
        }
        fail("Unexpected error during engine execution", e)
    }
}

fun validate(
    fixture: TestFixture,
    hook: EngineTestHook,
) {
    val validation = NadelSchemaValidation(
        NadelSchemas.Builder()
            .overallSchemas(fixture.overallSchema)
            .underlyingSchemas(fixture.underlyingSchema)
            .overallWiringFactory(GatewaySchemaWiringFactory())
            .underlyingWiringFactory(GatewaySchemaWiringFactory())
            .stubServiceExecution()
            .build()
    )

    val errors = validation.validate()
    if (errors.isEmpty()) {
        return
    }

    errors
        .asSequence()
        .map(NadelSchemaValidationError::message)
        .forEach(System.err::println)

    if (!hook.isSchemaValid(errors)) {
        throw ValidationException("Test fixture was not valid", errors)
    }
}
