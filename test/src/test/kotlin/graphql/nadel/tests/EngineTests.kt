package graphql.nadel.tests

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.GsonBuilder
import graphql.introspection.Introspection
import graphql.language.AstPrinter
import graphql.language.AstSorter
import graphql.language.Document
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionEngineFactory
import graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import graphql.nadel.Service
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.util.AnyList
import graphql.nadel.enginekt.util.AnyMap
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.enginekt.util.copyWithChildren
import graphql.nadel.enginekt.util.mapFrom
import graphql.nadel.enginekt.util.queryPath
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.enginekt.util.toBuilder
import graphql.nadel.tests.NadelEngineType.nextgen
import graphql.nadel.tests.util.getAncestorFile
import graphql.nadel.tests.util.requireIsDirectory
import graphql.nadel.tests.util.toSlug
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperation
import graphql.normalized.ExecutableNormalizedOperationFactory
import graphql.normalized.ExecutableNormalizedOperationToAstCompiler
import graphql.schema.GraphQLSchema
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestContext
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.fail
import java.io.File
import java.util.concurrent.CompletableFuture

private val serviceCalls = mutableListOf<ServiceCall>()

class EngineTests : FunSpec({
    val engineFactories = EngineTypeFactories()

    val fixturesDir = File(javaClass.classLoader.getResource("fixtures")!!.path)
        // Note: resources end up in nadel/test/build/resources/test/fixtures/
        .getAncestorFile("build").parentFile
        .requireIsDirectory()
        .let { moduleRootDir ->
            File(moduleRootDir, "src/test/resources/fixtures/").requireIsDirectory()
        }


    fixturesDir.listFiles()!!
        .asSequence()
        .filter {
            when (it.nameWithoutExtension) {
                // Field forbidden
                "hydrated-field-is-removed",
                "nested-hydrated-field-is-removed",
                "field-is-removed-from-nested-hydrated-field",
                "all-fields-in-a-selection-set-are-removed",
                "field-in-a-selection-set-is-removed",
                "one-of-top-level-fields-is-removed",
                "top-level-field-is-removed",
                "top-level-field-in-batched-query-is-removed",
                "all-fields-are-removed-from-hydrated-field",
                "field-is-removed-from-hydrated-field",
                "all-non-hydrated-fields-in-query-are-removed",
                "field-with-selections-is-removed",
                "the-only-field-in-a-selection-set-is-removed",
                "field-in-non-hydrated-query-is-removed",
                "restricted-field-inside-hydration-via-fragments-used-twice",
                "restricted-field-via-fragments-used-twice",
                "inserts-one-error-for-a-forbidden-field-in-a-list",
                "restricted-single-field-inside-hydration-via-fragments-used-twice",
                "restricted-single-field-via-fragments-used-twice",
                    // Dynamic service resolution
                "dynamic-service-resolution-multiple-services",
                "dynamic-service-resolution-simple-success-case",
                "dynamic-service-resolution-multiple-services-with-one-unmapped-node-lookup",
                "dynamic-service-resolution-handles-inline-fragments-from-multiple-services",
                "dynamic-service-resolution-handles-complex-fragments",
                "dynamic-service-resolution-with-no-fragments",
                "dynamic-service-resolution-directive-not-in-interface",
                "typename-is-passed-on-queries-using-dynamic-resolved-services",
                -> false
                else -> true
            }
        }
        .onEach {
            println("Loading ${it.nameWithoutExtension}")
        }
        .forEach { file ->
            file
                .let(File::readText)
                .let<String, TestFixture>(yamlObjectMapper::readValue)
                .also { fixture ->
                    // Rename fixtures if they have the wrong name
                    val expectedName = fixture.name.toSlug()
                    if (file.nameWithoutExtension != expectedName) {
                        file.renameTo(File(fixturesDir, "$expectedName.yml"))
                    }
                }
                // Execute the tests on all applicable engines
                .also { fixture ->
                    engineFactories.all
                        .asSequence()
                        .filter { (engine) ->
                            true
                            // fixture.name == "simple synthetic hydration with one service and type renaming"
                        }
                        .filter { (engine) ->
                            fixture.enabled.get(engine = engine) == null && engine == nextgen
                        }
                        .forEach { (engine, engineFactory) ->
                            // Run for tests that don't have nextgen calls
                            if (fixture.serviceCalls.nextgen.isNotEmpty()) {
                                return@forEach
                            }
                            val execute: suspend TestContext.() -> Unit = {
                                execute(file, fixture, engine, engineFactory)
                            }
                            serviceCalls.clear()
                            if (fixture.ignored.get(engine = engine)) {
                                xtest("$engine ${fixture.name}", execute)
                            } else {
                                test("$engine ${fixture.name}", execute)
                            }
                        }
                }
        }
})

private suspend fun execute(
    file: File,
    fixture: TestFixture,
    engineType: NadelEngineType,
    engineFactory: NadelExecutionEngineFactory,
) {
    println("Running ${fixture.name}")

    val testHooks = getTestHook(fixture)
    val serviceCalls = fixture.serviceCalls[engineType].toMutableList()

    val services = mutableMapOf<String, Service>()
    val nadel = Nadel.newNadel()
        .engineFactory(engineFactory)
        .dsl(fixture.overallSchema)
        .serviceExecutionFactory(object : ServiceExecutionFactory {
            private val astSorter = AstSorter()

            private val callsByTopLevelField by lazy {
                fixture.serviceCalls.current.strictAssociateBy { call ->
                    val service = services[call.serviceName] ?: error("No matching service")

                    val nq = ExecutableNormalizedOperationFactory.createExecutableNormalizedOperationWithRawVariables(
                        service.underlyingSchema, /* schema */
                        call.request.document, /* query */
                        null, /* operation name */
                        call.request.variables, /* variables */
                    )

                    toMatchableTopLevelCall(service.underlyingSchema, nq)
                }
            }

            private fun getMatchingCall(query: Document, serviceName: String): ServiceCall? {
                val service = services[serviceName] ?: error("No matching service")

                val nq = ExecutableNormalizedOperationFactory.createExecutableNormalizedOperationWithRawVariables(
                    service.underlyingSchema, /* schema */
                    query, /* query */
                    null, /* operation name */
                    emptyMap(), /* variables */
                )

                return callsByTopLevelField[
                    toMatchableTopLevelCall(service.underlyingSchema, nq),
                ]
            }

            private fun toMatchableTopLevelCall(
                schema: GraphQLSchema,
                nq: ExecutableNormalizedOperation,
            ): String {
                fun strip(nf: ExecutableNormalizedField): ExecutableNormalizedField {
                    return nf.toBuilder()
                        .alias(null)
                        .children(emptyList())
                        .build()
                }

                return AstPrinter.printAst(
                    ExecutableNormalizedOperationToAstCompiler.compileToDocument(
                        nq.topLevelFields
                            .asSequence()
                            .map {
                                it to strip(it)
                            }
                            .map { (field, stripped) ->
                                if (stripped.resolvedArguments.isEmpty()) {
                                    stripped.copyWithChildren(
                                        field.children
                                            .filterNot { it.name == Introspection.TypeNameMetaFieldDef.name }
                                            .map(::strip),
                                    )
                                } else {
                                    stripped
                                }
                            }
                            .toList()
                    )
                )
            }

            private fun transform(
                incomingQuery: Document,
                response: JsonMap,
                serviceName: String,
            ): JsonMap {
                val service = services[serviceName] ?: error("No matching service")

                val nq = ExecutableNormalizedOperationFactory.createExecutableNormalizedOperationWithRawVariables(
                    service.underlyingSchema, /* schema */
                    incomingQuery, /* query */
                    null, /* operation name */
                    emptyMap(), /* variables */
                )

                println("Original service response")
                println(response)
                println()

                @Suppress("UNCHECKED_CAST")
                return response.mapValues { (key, value) ->
                    when (value) {
                        null -> null
                        else -> when (key) {
                            "data" -> transform(nq.topLevelFields, value as JsonMap, service.underlyingSchema)
                            else -> value
                        }
                    }
                }.also {
                    println("Mapped service response")
                    println(it)
                    println()
                }
            }

            private fun transform(
                fields: List<ExecutableNormalizedField>,
                parent: JsonMap,
                underlyingSchema: GraphQLSchema,
            ): JsonMap {
                return mapFrom(
                    fields.mapNotNull { field ->
                        if ("type_hint_typename__UUID" in parent && parent["type_hint_typename__UUID"] !in field.objectTypeNames) {
                            return@mapNotNull null
                        }

                        field.resultKey to (if (field.name in parent) {
                            parent[field.name]
                        } else if (field.name == Introspection.TypeNameMetaFieldDef.name) {
                            if ("type_hint_typename__UUID" in parent) {
                                parent["type_hint_typename__UUID"] as String
                            } else {
                                field.objectTypeNames.single().also {
                                    println("Inferring type of ${field.queryPath} as $it")
                                }
                            }
                        } else if ("__object_ID__" in field.resultKey) {
                            if ("object_identifier__UUID" in parent) {
                                parent["object_identifier__UUID"]
                            } else {
                                fail("Could not determine object identifier")
                            }
                        } else {
                            fail("Could not determine value")
                        }).let { value ->
                            fun mapValue(value: Any?): Any? {
                                @Suppress("UNCHECKED_CAST")
                                return when (value) {
                                    is AnyList -> value.map(::mapValue)
                                    is AnyMap -> transform(
                                        fields = field.children,
                                        parent = value as JsonMap,
                                        underlyingSchema = underlyingSchema,
                                    )
                                    else -> value
                                }
                            }

                            mapValue(value)
                        }
                    },
                )
            }

            private fun <K, V> Map<K, V>.deepClone(): Map<K, V> {
                return mapValues { (_, value) ->
                    @Suppress("UNCHECKED_CAST")
                    when (value) {
                        is AnyList -> value.deepClone() as V
                        is AnyMap -> value.deepClone() as V
                        else -> value
                    }
                }
            }

            private fun <T> List<T>.deepClone(): List<T> {
                return map { value ->
                    @Suppress("UNCHECKED_CAST")
                    when (value) {
                        is AnyMap -> value.deepClone() as T
                        is AnyList -> value.deepClone() as T
                        else -> value
                    }
                }
            }

            override fun getServiceExecution(serviceName: String): ServiceExecution {
                return ServiceExecution { params ->
                    try {
                        val incomingQuery = params.query
                        val incomingQueryPrinted = AstPrinter.printAst(
                            astSorter.sort(incomingQuery),
                        )
                        println(incomingQueryPrinted)

                        val response = synchronized(serviceCalls) {
                            val indexOfCall = serviceCalls
                                .indexOfFirst {
                                    it.serviceName == serviceName && AstPrinter.printAst(it.request.document) == incomingQueryPrinted
                                }
                                .takeIf { it != -1 }

                            val response: JsonMap = if (indexOfCall != null) {
                                serviceCalls.removeAt(indexOfCall).response
                            } else {
                                var match: JsonMap? = null
                                if (engineType == nextgen) {
                                    println("Attempting to match query")
                                    val matchingCall = getMatchingCall(incomingQuery, serviceName)
                                    if (matchingCall != null) {
                                        val response = matchingCall.response
                                        match = transform(incomingQuery, response, serviceName)
                                    }
                                }

                                match
                                    ?: fail("Unable to match service call")
                            }

                            serviceCalls.add(ServiceCall(
                                serviceName = serviceName,
                                request = ServiceCall.Request(
                                    incomingQueryPrinted,
                                    variables = emptyMap(),
                                    operationName = null,
                                ),
                                responseJsonString = GsonBuilder()
                                    .setPrettyPrinting()
                                    .serializeNulls()
                                    .create()
                                    .toJson(response)
                            ))

                            response
                        }

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
            testHooks?.makeNadel(engineType, it) ?: it
        }
        .build()
        .also { nadel ->
            nadel.services.forEach { service ->
                services[service.name] = service
            }
        }

    val response = try {
        nadel.execute(newNadelExecutionInput()
            .query(fixture.query)
            .variables(fixture.variables)
            .artificialFieldsUUID("UUID")
            .let { builder ->
                testHooks?.makeExecutionInput(engineType, builder) ?: builder
            }
            .build())
            .await()
    } catch (e: Exception) {
        if (fixture.exception != null) {
            if (fixture.exception.message.matches(e.message ?: "")) {
                // Pass test
                val yaml = yamlObjectMapper.writeValueAsString(
                    fixture.copy(
                        serviceCalls = fixture.serviceCalls.copy(
                            nextgen = serviceCalls,
                        ),
                    ),
                )
                // file.writeText(yaml)
                return
            }
        }
        fail("Unexpected error during engine execution", e)
    }

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
                    println("Overall response")
                    println(it)
                    println()
                }
            },
            expected = expectedResponse.let {
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

    testHooks?.assertResult(engineType, response)

    println()
    println()
    println("New test fixture")
    val yaml = yamlObjectMapper.writeValueAsString(
        fixture.copy(
            serviceCalls = fixture.serviceCalls.copy(
                nextgen = serviceCalls,
            ),
        ),
    )
    // file.writeText(yaml)
    println(yaml)
}
