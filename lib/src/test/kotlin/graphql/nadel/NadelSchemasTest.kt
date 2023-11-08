package graphql.nadel

import graphql.language.NamedNode
import graphql.nadel.engine.util.getField
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.schema.ServiceSchemaProblem
import graphql.nadel.validation.util.NadelBuiltInTypes.allNadelBuiltInTypeNames
import graphql.schema.GraphQLSchema
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.kotest.core.spec.style.DescribeSpec
import org.junit.jupiter.api.assertThrows

val NadelDefinitionRegistry.typeNames: Set<String>
    get() = definitions
        .asSequence()
        .filterIsInstance<NamedNode<*>>()
        .map { it.name }
        .toSet()

/**
 * Type names excluding built in types.
 */
val GraphQLSchema.userTypeNames: Set<String>
    get() = typeMap.keys
        .asSequence()
        .filterNot {
            it.startsWith("__")
        }
        .filterNot {
            it in allNadelBuiltInTypeNames
        }
        .toSet()

class NadelSchemasTest : DescribeSpec({
    describe("build") {
        it("generates valid overall schema") {
            val overallSchema = mapOf(
                "test" to """
                    type Query {
                        echo: Echo
                    }
                    type Echo {
                        world: World
                    }
                    type World {
                        hello: String
                    }
                """.trimIndent(),
            )
            val underlyingSchema = mapOf(
                "test" to """
                    type Query {
                        echo: Echo
                    }
                    type Echo {
                        world: World
                    }
                    type World {
                        hello: String
                    }
                    type Food {
                        isTasty: Boolean
                    }
                """.trimIndent(),
            )

            // when
            val schemas = NadelSchemas.newNadelSchemas()
                .overallSchemas(overallSchema)
                .underlyingSchemas(underlyingSchema)
                .stubServiceExecution()
                .build()

            // then
            assert(schemas.engineSchema.userTypeNames == setOf("World", "Echo", "Query", "JSON", "NadelWhenCondition", "NadelWhenConditionPredicate", "NadelWhenConditionResult"))
            val testService = schemas.services.single()
            assert(testService.underlyingSchema.userTypeNames == setOf("World", "Echo", "Query", "Food"))
        }

        it("throws wrapping ServiceSchemaProblem") {
            val overallSchema = mapOf(
                "test" to """
                    type Query {
                        echo: String
                    }
                """.trimIndent(),
            )
            val underlyingSchema = mapOf(
                "test" to """
                    type QueryMissing {
                        echo: String
                    }
                """.trimIndent(),
            )

            // when
            val ex = assertThrows<ServiceSchemaProblem> {
                NadelSchemas.newNadelSchemas()
                    .overallSchemas(overallSchema)
                    .underlyingSchemas(underlyingSchema)
                    .stubServiceExecution()
                    .build()
            }

            // then
            assert(ex.serviceName == "test")
            assert(ex.message.contains("A schema MUST have a 'query' operation defined"))
        }

        it("works if you exclusively supply type defs") {
            val overallSchema = mapOf(
                "test" to """
                    type Query {
                        echo: Echo
                    }
                    type Echo {
                        world: World
                    }
                    type World {
                        hello: String
                    }
                """.trimIndent(),
            )
            val underlyingSchema: Map<String, TypeDefinitionRegistry> = mapOf(
                "test" to """
                    type Query {
                        echo: Echo
                    }
                    type Echo {
                        world: World
                    }
                    type World {
                        hello: String
                    }
                    type Food {
                        isTasty: Boolean
                    }
                """.trimIndent(),
            ).mapValues { (_, schemaText) ->
                SchemaParser().parse(schemaText)
            }

            // when
            val schemas = NadelSchemas.newNadelSchemas()
                .overallSchemas(overallSchema)
                .underlyingSchemas(underlyingSchema)
                .stubServiceExecution()
                .build()

            // then
            assert(schemas.engineSchema.userTypeNames == setOf("World", "Echo", "Query", "JSON", "NadelWhenCondition", "NadelWhenConditionPredicate", "NadelWhenConditionResult"))
            val testService = schemas.services.single()
            assert(testService.underlyingSchema.userTypeNames == setOf("World", "Echo", "Query", "Food"))
        }

        it("works if you supply both type defs and readers") {
            val overallSchema = mapOf(
                "test" to """
                    type Query {
                        echo: Echo
                    }
                    type Echo {
                        world: World
                    }
                    type World {
                        hello: String
                    }
                """.trimIndent(),
                "issue" to """
                    type Query {
                        issue: Issue
                    }
                    type Issue {
                        id: ID!
                    }
                """.trimIndent(),
            )
            val underlyingSchemaDefs: Map<String, TypeDefinitionRegistry> = mapOf(
                "test" to """
                    type Query {
                        echo: Echo
                    }
                    type Echo {
                        world: World
                    }
                    type World {
                        hello: String
                    }
                """.trimIndent(),
            ).mapValues { (_, schemaText) ->
                SchemaParser().parse(schemaText)
            }
            val underlyingSchemaStrings: Map<String, String> = mapOf(
                "issue" to """
                    type Query {
                        issue: Issue
                    }
                    type Issue {
                        id: ID!
                    }
                """.trimIndent(),
            )

            // when
            val schemas = NadelSchemas.newNadelSchemas()
                .overallSchemas(overallSchema)
                .underlyingSchemas(underlyingSchemaDefs)
                .underlyingSchemas(underlyingSchemaStrings)
                .stubServiceExecution()
                .build()

            // then
            assert(schemas.engineSchema.userTypeNames == setOf("World", "Echo", "Query", "Issue", "JSON", "NadelWhenCondition", "NadelWhenConditionPredicate", "NadelWhenConditionResult"))

            val issueService = schemas.services.single { it.name == "issue" }
            assert(issueService.underlyingSchema.userTypeNames == setOf("Query", "Issue"))

            val testService = schemas.services.single { it.name == "test" }
            assert(testService.underlyingSchema.userTypeNames == setOf("Query", "Echo", "World"))
        }

        it("combines the overall schemas") {
            val overallSchema = mapOf(
                "test" to """
                    type Query {
                        echo: Echo
                    }
                    type Echo {
                        world: World
                    }
                    type World {
                        hello: String
                    }
                """.trimIndent(),
                "issue" to """
                    type Query {
                        issue: Issue
                    }
                    type Issue {
                        id: ID!
                    }
                """.trimIndent(),
            )
            val underlyingSchema = mapOf(
                "test" to """
                    type Query {
                        echo: Echo
                    }
                    type Echo {
                        world: World
                    }
                    type World {
                        hello: String
                    }
                """.trimIndent(),
                "issue" to """
                    type Query {
                        issue: Issue
                    }
                    type Issue {
                        id: ID!
                    }
                """.trimIndent(),
            )

            // when
            val schemas = NadelSchemas.newNadelSchemas()
                .overallSchemas(overallSchema)
                .underlyingSchemas(underlyingSchema)
                .stubServiceExecution()
                .build()

            // then
            assert(schemas.engineSchema.userTypeNames == setOf("World", "Echo", "Query", "Issue", "JSON", "NadelWhenCondition", "NadelWhenConditionPredicate", "NadelWhenConditionResult"))
        }

        it("does not validate the schemas") {
            val overallSchema = mapOf(
                "test" to """
                    type Query {
                        echo: Echo
                    }
                    type Echo {
                        world: World
                    }
                    type World {
                        hello: String
                    }
                """.trimIndent(),
                "issue" to """
                    type Query {
                        issue: Issue
                    }
                    type Issue {
                        id: ID!
                    }
                """.trimIndent(),
            )
            val underlyingSchema = mapOf(
                "test" to """
                    type Query {
                        echo: String
                    }
                """.trimIndent(),
                "issue" to """
                    type Query {
                        issue: Task
                    }
                    type Task {
                        id: ID!
                    }
                """.trimIndent(),
            )

            // when
            val schemas = NadelSchemas.newNadelSchemas()
                .overallSchemas(overallSchema)
                .underlyingSchemas(underlyingSchema)
                .stubServiceExecution()
                .build()

            // then
            assert(schemas.engineSchema.userTypeNames == setOf("Query", "World", "Echo", "Issue", "JSON", "NadelWhenCondition", "NadelWhenConditionPredicate", "NadelWhenConditionResult"))

            val testService = schemas.services.first { it.name == "test" }
            assert(testService.definitionRegistry.typeNames == setOf("Query", "Echo", "World"))
            assert(testService.underlyingSchema.userTypeNames == setOf("Query"))

            val issueService = schemas.services.first { it.name == "issue" }
            assert(issueService.underlyingSchema.userTypeNames == setOf("Query", "Task"))
            assert(issueService.definitionRegistry.typeNames == setOf("Query", "Issue"))
        }

        it("retains line numbers for string input") {
            val overallSchema = mapOf(
                "test" to """
                    type Query {
                        echo: Echo
                    }
                    type Echo {
                        world: String 
                    }
                """.trimIndent(),
                "issue" to """
                    type Query {
                        issue: Issue
                    }
                    type Issue {
                        # This is a comment
                        id: ID!
                    }
                """.trimIndent(),
            )
            val underlyingSchema = mapOf(
                "test" to """
                    type Query {
                        echo: Echo
                    }
                   
                    type Echo {
                        world: String 
                    }
                """.trimIndent(),
                "issue" to """
                    type Query {
                        issue: Task
                    }
                    type Task {
                        id: ID!
                    }
                """.trimIndent(),
            )

            val echoCoordinates = makeFieldCoordinates("Query", "echo")
            val worldCoordinates = makeFieldCoordinates("Echo", "world")
            val issueIdCoordinates = makeFieldCoordinates("Issue", "id")

            // when
            val schemas = NadelSchemas.newNadelSchemas()
                .overallSchemas(overallSchema)
                .underlyingSchemas(underlyingSchema)
                .stubServiceExecution()
                .build()

            // then
            assert(schemas.engineSchema.typeMap["Echo"]?.definition?.sourceLocation?.line == 4)
            assert(schemas.engineSchema.typeMap["Issue"]?.definition?.sourceLocation?.line == 4)
            assert(schemas.engineSchema.getField(echoCoordinates)?.definition?.sourceLocation?.line == 2)
            assert(schemas.engineSchema.getField(worldCoordinates)?.definition?.sourceLocation?.line == 5)
            assert(schemas.engineSchema.getField(issueIdCoordinates)?.definition?.sourceLocation?.line == 6)

            val testService = schemas.services.single { it.name == "test" }
            assert(testService.underlyingSchema.typeMap["Echo"]?.definition?.sourceLocation?.line == 5)
            assert(testService.underlyingSchema.getField(echoCoordinates)?.definition?.sourceLocation?.line == 2)
            assert(testService.underlyingSchema.getField(worldCoordinates)?.definition?.sourceLocation?.line == 6)

            val issueService = schemas.services.single { it.name == "issue" }
            assert(issueService.underlyingSchema.typeMap["Task"]?.definition?.sourceLocation?.line == 4)
        }
    }
})
