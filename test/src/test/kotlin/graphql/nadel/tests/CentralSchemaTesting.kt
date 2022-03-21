package graphql.nadel.tests

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import graphql.GraphQLError
import graphql.language.AstPrinter
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput.Companion.newNadelExecutionInput
import graphql.nadel.NextgenEngine
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.enginekt.util.MutableJsonMap
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.validation.NadelSchemaValidation
import graphql.nadel.validation.NadelSchemaValidationError
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import kotlinx.coroutines.future.asDeferred
import java.io.File
import java.util.concurrent.CompletableFuture

val File.parents: Sequence<File>
    get() = sequence<File> {
        var file: File? = parentFile
        while (file != null) {
            yield(file)
            file = file.parentFile
        }
    }

/**
 * By default, we don't actually run any queries against the Nadel instance.
 *
 * Set this to true to run the query. You'll have to modify the [ServiceExecutionFactory] to
 * actually return something e.g. response from Splunk etc.
 */
const val runQuery = false

/**
 * You can use this script to run central schema locally in Nadel without
 * booting up the full Gateway.
 *
 * This can be used for debugging Nadel instantiation, query execution, and
 * running the latest validation against central schema.
 *
 * Remember that this is NOT the exact same as running the full Gateway.
 * This is only here for convenience as the full Gateway is a little slower
 * to debug.
 */
suspend fun main() {
    val schema = File(
        "/Users/fwang/Documents/GraphQL/graphql-central-schema/schema/",
    )
    val overallSchemas = mutableMapOf<String, String>()
    val underlyingSchemas = mutableMapOf<String, String>()

    schema.walkTopDown()
        .mapNotNull { file ->
            val serviceName = file.parents
                .takeWhile {
                    it.absolutePath != schema.absolutePath
                }
                .firstOrNull { parent ->
                    parent.listFiles()?.any { it.name == "config.yaml" || it.name == "config.yml" } ?: false
                }
                ?.name

            file to (serviceName ?: return@mapNotNull null)
        }
        .forEach { (file, serviceName) ->
            if (file.extension == "nadel") {
                if (file.parentFile.name in overallSchemas) {
                    overallSchemas[serviceName] += file.readText()
                } else {
                    overallSchemas[serviceName] = file.readText()
                }
            } else if (file.extension == "graphqls" || file.extension == "graphql") {
                val text = file.readText()
                underlyingSchemas.compute(serviceName) { _, oldValue ->
                    if (oldValue == null) {
                        text + "\n"
                    } else {
                        oldValue + "\n" + text + "\n"
                    }
                }
            }
        }

    require(overallSchemas.keys == underlyingSchemas.keys)
    // println(overallSchemas.keys)

    val nadel = Nadel.newNadel()
        .engineFactory { nadel ->
            NextgenEngine(nadel)
            // NadelEngine(nadel)
        }
        .dsl(overallSchemas)
        .serviceExecutionFactory(object : ServiceExecutionFactory {
            override fun getServiceExecution(serviceName: String): ServiceExecution {
                return ServiceExecution {
                    AstPrinter.printAst(it.query).also(::println)

                    val response: JsonMap = ObjectMapper().readValue(
                        File("/Users/fwang/Library/Application Support/JetBrains/IntelliJIdea2021.1/scratches/buffer2.kts")
                            .readText(),
                    )

                    CompletableFuture.completedFuture(
                        ServiceExecutionResult(
                            response["data"] as MutableJsonMap
                        ),
                    )
                }
            }

            override fun getUnderlyingTypeDefinitions(serviceName: String): TypeDefinitionRegistry {
                return SchemaParser().parse(underlyingSchemas[serviceName] ?: return TypeDefinitionRegistry())
            }
        })
        .overallWiringFactory(GatewaySchemaWiringFactory())
        .underlyingWiringFactory(GatewaySchemaWiringFactory())
        .build()

    NadelSchemaValidation(
        overallSchema = nadel.engineSchema,
        services = nadel.services.strictAssociateBy { it.name }
    )
        .validate()
        .sortedBy { it.javaClass.name }
        .asSequence()
        .map(NadelSchemaValidationError::toGraphQLError)
        .map(GraphQLError::toSpecification)
        .forEach(::println)

    @Suppress("ConstantConditionIf")
    if (!runQuery) {
        return
    }

    nadel
        .execute(
            newNadelExecutionInput()
                .query(query)
                .build(),
        )
        .asDeferred()
        .await()
        .also {
            println(it)
        }
}

const val query = ""
