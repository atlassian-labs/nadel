package graphql.nadel.tests.next.fixtures.execution

import graphql.ExecutionResult
import graphql.execution.instrumentation.InstrumentationContext
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLTypeUtil.simplePrint
import java.util.concurrent.CompletableFuture

class ExecutableNormalizedFieldCovariantTypeTest : NadelIntegrationTest(
    query = """
        query {
          holders {
            entity {
              id
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "entities",
            overallSchema = """
                directive @scopes(required: Scope!) on OBJECT

                enum Scope {
                  HUMAN
                  ANIMAL
                }

                interface Entity {
                  id: ID!
                }
                type Human implements Entity @scopes(required: HUMAN) {
                  id: ID!
                }
                type Animal implements Entity @scopes(required: ANIMAL) {
                  id: ID!
                }

                interface EntityHolder {
                  entity: Entity
                }
                type HumanHolder implements EntityHolder {
                  entity: Entity
                }
                type AnimalHolder implements EntityHolder {
                  entity: Animal
                }

                type Query {
                  holders: [EntityHolder!]!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Human(
                    val id: String,
                )

                data class Animal(
                    val id: String,
                )

                data class HumanHolder(
                    val entity: Human,
                )

                data class AnimalHolder(
                    val entity: Animal,
                )

                wiring
                    .type("EntityHolder") { type ->
                        type
                            .typeResolver { env ->
                                env.schema.getObjectType(env.getObject<Any>().javaClass.simpleName)
                            }
                    }
                    .type("Entity") { type ->
                        type
                            .typeResolver { env ->
                                env.schema.getObjectType(env.getObject<Any>().javaClass.simpleName)
                            }
                    }
                    .type("Query") { type ->
                        type
                            .dataFetcher("holders") { env ->
                                listOf(
                                    HumanHolder(
                                        entity = Human(id = "human-1"),
                                    ),
                                    AnimalHolder(
                                        entity = Animal(id = "animal-1"),
                                    ),
                                )
                            }
                    }
            },
        ),
    ),
) {
    override fun makeInstrumentation(): NadelInstrumentation {
        return object : NadelInstrumentation {
            override fun beginExecute(
                parameters: NadelInstrumentationExecuteOperationParameters,
            ): CompletableFuture<InstrumentationContext<ExecutionResult>> {
                parameters.normalizedOperation.topLevelFields.forEach { field ->
                    printExecutableNormalizedField(
                        field = field,
                        schema = parameters.graphQLSchema,
                    )
                }
                return super.beginExecute(parameters)
            }
        }
    }

    private fun printExecutableNormalizedField(
        field: ExecutableNormalizedField,
        schema: GraphQLSchema,
        indentation: String = "",
    ) {
        val objectTypes = field.objectTypeNames.joinToString(
            separator = ", ",
            prefix = "[",
            postfix = "]",
        )
        val fieldTypes = field.getFieldDefinitions(schema).joinToString(
            separator = ", ",
            prefix = "[",
            postfix = "]",
        ) { fieldDefinition ->
            simplePrint(fieldDefinition.type)
        }
        println("$indentation$objectTypes.${field.fieldName}: $fieldTypes")
        field.children.forEach { child ->
            printExecutableNormalizedField(
                field = child,
                schema = schema,
                indentation = "$indentation  ",
            )
        }
    }
}
