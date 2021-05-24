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
import graphql.nadel.enginekt.util.JsonMap
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.future.await
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
            test(fixture.name) {
                execute(fixture)
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
                    val incomingQuery = AstPrinter.printAst(
                        astSorter.sort(params.query)
                    )
                    val call = fixture.calls.single {
                        AstPrinter.printAst(it.request.document) == incomingQuery
                    }

                    @Suppress("UNCHECKED_CAST")
                    CompletableFuture.completedFuture(
                        ServiceExecutionResult(
                            call.response["data"] as JsonMap,
                        ),
                    )
                }
            }

            override fun getUnderlyingTypeDefinitions(serviceName: String): TypeDefinitionRegistry {
                return SchemaParser().parse(fixture.services[serviceName])
            }
        })
        .build()

    val response = nadel.execute(newNadelExecutionInput()
        .query(fixture.query)
        .variables(fixture.variables)
        .build())
        .await()

    println(jsonObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.toSpecification()))
}

private data class TestFixture(
    val name: String,
    val overallSchema: Map<String, String>,
    val services: Map<String, String>,
    val query: String,
    val variables: Map<String, Any?>,
    val calls: List<ServiceCall>,
    @JsonProperty("response")
    private val responseJsonString: String,
) {
    val response: JsonMap by lazy {
        jsonObjectMapper.readValue(responseJsonString)
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
