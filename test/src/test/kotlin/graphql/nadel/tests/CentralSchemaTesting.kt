package graphql.nadel.tests

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import graphql.GraphQLError
import graphql.language.AstPrinter
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import graphql.nadel.NextgenEngine
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.schema.NeverWiringFactory
import graphql.nadel.validation.NadelSchemaValidation
import graphql.nadel.validation.NadelSchemaValidationError
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import kotlinx.coroutines.future.asDeferred
import java.io.File
import java.util.concurrent.CompletableFuture

/**
 * Run this and replace the path to central schema to load it up in Nadel.
 */
suspend fun main() {
    val schema = File(
        "/Users/fwang/Documents/GraphQL/graphql-central-schema/schema/",
    )
    val overallSchemas = mutableMapOf<String, String>()
    val underlyingSchemas = mutableMapOf<String, String>()

    schema.walkTopDown().forEach { file ->
        if (file.extension == "nadel") {
            if (file.parentFile.name in overallSchemas) {
                overallSchemas[file.parentFile.name] += file.readText()
            } else {
                overallSchemas[file.parentFile.name] = file.readText()
            }
        } else if (file.extension == "graphqls") {
            val text = file.readText()
            underlyingSchemas.compute(file.parentFile.name) { _, oldValue ->
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
                            response["data"] as JsonMap
                        ),
                    )
                }
            }

            override fun getUnderlyingTypeDefinitions(serviceName: String): TypeDefinitionRegistry {
                return SchemaParser().parse(underlyingSchemas[serviceName] ?: return TypeDefinitionRegistry())
            }
        })
        .build()

    NadelSchemaValidation(
        overallSchema = nadel.overallSchema,
        services = nadel.services.strictAssociateBy { it.name }
    )
        .validate()
        .sortedBy { it.javaClass.name }
        .asSequence()
        .map(NadelSchemaValidationError::toGraphQLError)
        .map(GraphQLError::toSpecification)
        .forEach(::println)

    @Suppress("ConstantConditionIf")
    if (true) {
        return
    }

    nadel
        .execute(
            newNadelExecutionInput()
                .artificialFieldsUUID("UUID")
                .query(query)
                .build(),
        )
        .asDeferred()
        .await()
        .also {
            println(it)
        }
}

class GatewaySchemaWiringFactory : NeverWiringFactory()

const val query = ""
