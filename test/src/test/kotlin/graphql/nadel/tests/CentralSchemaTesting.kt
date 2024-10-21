package graphql.nadel.tests

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import graphql.GraphQLError
import graphql.language.AstPrinter
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput.Companion.newNadelExecutionInput
import graphql.nadel.NadelSchemas
import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.validation.NadelSchemaValidation
import graphql.nadel.validation.NadelSchemaValidationError
import kotlinx.coroutines.future.asDeferred
import java.io.File
import java.util.concurrent.CompletableFuture

val File.parents: Sequence<File>
    get() = sequence {
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
        "/Users/fwang/Documents/Atlassian/graphql-central-schema/schema/",
    )
    val overallSchemas = mutableMapOf<String, String>()
    val underlyingSchemas = mutableMapOf<String, String>()

    schema.walkTopDown()
        .filter {
            it.isFile
        }
        .mapNotNull { file ->
            val relativeFilePath = file.absolutePath.removePrefix(schema.absolutePath).trimStart('/')
            val serviceName = getServiceName(File(relativeFilePath))

            file to (serviceName ?: return@mapNotNull null)
        }
        .forEach { (file, serviceName) ->
            fun MutableMap<String, String>.append(text: String) {
                compute(serviceName) { _, oldValue ->
                    if (oldValue == null) {
                        text + "\n"
                    } else {
                        oldValue + "\n" + text + "\n"
                    }
                }
            }

            if (file.extension == "nadel") {
                overallSchemas.append(file.readText())
            } else if (file.extension == "graphqls" || file.extension == "graphql") {
                underlyingSchemas.append(file.readText())
            }
        }

    val nadel = Nadel.newNadel()
        .overallSchemas(overallSchemas)
        .underlyingSchemas(underlyingSchemas)
        .serviceExecutionFactory(object : ServiceExecutionFactory {
            override fun getServiceExecution(serviceName: String): ServiceExecution {
                return ServiceExecution {
                    AstPrinter.printAst(it.query).also(::println)

                    val response: JsonMap = ObjectMapper().readValue(
                        File("/Users/fwang/Library/Application Support/JetBrains/IntelliJIdea2021.1/scratches/buffer2.kts")
                            .readText(),
                    )

                    CompletableFuture.completedFuture(
                        NadelServiceExecutionResultImpl(
                            response["data"] as MutableJsonMap
                        ),
                    )
                }
            }
        })
        .overallWiringFactory(GatewaySchemaWiringFactory())
        .underlyingWiringFactory(GatewaySchemaWiringFactory())
        .build()

    NadelSchemaValidation(
        NadelSchemas.Builder()
            .overallSchemas(overallSchemas)
            .underlyingSchemas(underlyingSchemas)
            .overallWiringFactory(GatewaySchemaWiringFactory())
            .underlyingWiringFactory(GatewaySchemaWiringFactory())
            .stubServiceExecution()
            .build()
    )
        .validate()
        .sortedBy { it.javaClass.name }
        .asSequence()
        .filterIsInstance<NadelSchemaValidationError>()
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

private fun getServiceName(file: File): String? {
    val parts: List<File> = splitFileParts(file)
    val partCount = parts.size

    if (partCount <= 1 || partCount > 4) {
        return null
    }

    if (partCount == 4) {
        // it might be a <serviceGroup>/<serviceName>/underlying|overall/some.file
        val underlyingOrOverallDirPart = parts[2].name
        return if (underlyingOrOverallDirPart == UNDERLYING || underlyingOrOverallDirPart == OVERALL) {
            parts[1].name
        } else null
    }

    if (partCount == 3) {
        // it might be a <serviceGroup>/<serviceName>/some.file
        // OR
        // <serviceName>/underlying|overall/some.file
        val name = parts[1].name
        return if (name == UNDERLYING || name == OVERALL) {
            parts[0].name
        } else name
    }

    return if (parts[1].name == SHARED_DOT_NADEL) {
        // it might be a <serviceGroup>/shared.nadel
        SHARED
    } else {
        // it must be a <serviceName>/some.file
        parts[0].name
    }
}

const val UNDERLYING = "underlying"
const val OVERALL = "overall"
const val SHARED_DOT_NADEL = "shared.nadel"
const val SHARED = "shared"
const val CONFIG_DOT_YAML = "config.yaml"
const val DOT_GRAPHQLS = ".graphqls"
const val DOT_GRAPHQL = ".graphql"
const val DOT_NADEL = ".nadel"

private fun splitFileParts(file: File): List<File> {
    var cursor = file

    val parts: MutableList<File> = ArrayList()
    parts.add(File(cursor.name))

    while (cursor.parentFile != null) {
        parts.add(cursor.parentFile)
        cursor = cursor.parentFile
    }

    parts.reverse()
    return parts
}
