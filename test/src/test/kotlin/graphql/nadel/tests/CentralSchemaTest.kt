package graphql.nadel.tests

import graphql.nadel.Nadel
import graphql.nadel.NextgenEngine
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy.MatchIndex
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.kotest.core.spec.style.DescribeSpec
import org.junit.jupiter.api.fail
import java.io.File

class CentralSchemaTest : DescribeSpec({
    it("runs") {
        val schema = File("/Users/fwang/Documents/GraphQL/graphql-central-schema/schema")
        val overallSchemas = mutableMapOf<String, String>()
        val underlyingSchemas = mutableMapOf<String, String>()

        schema.walkTopDown().forEach { file ->
            if (file.extension == "nadel") {
                overallSchemas[file.parentFile.name] = file.readText()
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
        println(overallSchemas.keys)

        lateinit var engine: NextgenEngine
        val nadel = Nadel.newNadel()
            .engineFactory { nadel ->
                NextgenEngine(nadel).also { engine = it }
            }
            .dsl(overallSchemas)
            .serviceExecutionFactory(object : ServiceExecutionFactory {
                override fun getServiceExecution(serviceName: String): ServiceExecution {
                    return ServiceExecution {
                        TODO("no-op")
                    }
                }

                override fun getUnderlyingTypeDefinitions(serviceName: String): TypeDefinitionRegistry {
                    return SchemaParser().parse(underlyingSchemas[serviceName] ?: error("No schema"))
                }
            })
            .build()

        println(engine)
        // engine.overallExecutionBlueprint.fieldInstructions
        //     .asSequence()
        //     .map { it.value }
        //     .filterIsInstance<NadelBatchHydrationFieldInstruction>()
        //     .forEach { instruction ->
        //         if (instruction.batchHydrationMatchStrategy is MatchIndex) {
        //             if (instruction.actorInputValueDefs.size != 1) {
        //                 fail("Hmm")
        //             }
        //         }
        //     }
    }
})

