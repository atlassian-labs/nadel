package graphql.nadel.tests

import com.fasterxml.jackson.module.kotlin.readValue
import graphql.language.AstPrinter
import graphql.language.AstSorter
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionEngineFactory
import graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.util.JsonMap
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestContext
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.fail
import java.io.File
import java.util.concurrent.CompletableFuture

class EngineTests : FunSpec({
    val engineFactories = EngineTypeFactories()
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
    engineType: NadelEngineType,
    engineFactory: NadelExecutionEngineFactory,
) {
    val testHooks = getTestHook(fixture)
    val serviceCalls = fixture.serviceCalls[engineType].toMutableList()

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

                        val response = synchronized(serviceCalls) {
                            val indexOfCall = serviceCalls
                                .indexOfFirst {
                                    it.serviceName == serviceName && AstPrinter.printAst(it.request.document) == incomingQueryPrinted
                                }
                                .takeIf { it != -1 }
                                ?: error("Unable to match service call")

                            serviceCalls.removeAt(indexOfCall).response
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
