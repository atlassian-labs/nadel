package graphql.nadel

import graphql.language.NamedNode
import graphql.language.SourceLocation
import graphql.nadel.engine.util.getField
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.schema.ServiceSchemaProblem
import graphql.nadel.validation.util.NadelBuiltInTypes.allNadelBuiltInTypeNames
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertTrue

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

class NadelSchemasTest {
    @Test
    fun `generates valid overall schema`() {
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
        assertTrue(schemas.engineSchema.userTypeNames == setOf("World", "Echo", "Query", "JSON"))
        val testService = schemas.services.single()
        assertTrue(testService.underlyingSchema.userTypeNames == setOf("World", "Echo", "Query", "Food"))
    }

    @Test
    fun `throws wrapping ServiceSchemaProblem`() {
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
        assertTrue(ex.serviceName == "test")
        assertTrue(ex.message.contains("A schema MUST have a 'query' operation defined"))
    }

    @Test
    fun `works if you exclusively supply type defs`() {
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
        assertTrue(schemas.engineSchema.userTypeNames == setOf("World", "Echo", "Query", "JSON"))
        val testService = schemas.services.single()
        assertTrue(testService.underlyingSchema.userTypeNames == setOf("World", "Echo", "Query", "Food"))
    }

    @Test
    fun `works if you supply both type defs and readers`() {
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
        assertTrue(schemas.engineSchema.userTypeNames == setOf("World", "Echo", "Query", "Issue", "JSON"))

        val issueService = schemas.services.single { it.name == "issue" }
        assertTrue(issueService.underlyingSchema.userTypeNames == setOf("Query", "Issue"))

        val testService = schemas.services.single { it.name == "test" }
        assertTrue(testService.underlyingSchema.userTypeNames == setOf("Query", "Echo", "World"))
    }

    @Test
    fun `combines the overall schemas`() {
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
        assertTrue(schemas.engineSchema.userTypeNames == setOf("World", "Echo", "Query", "Issue", "JSON"))
    }

    @Test
    fun `does not validate the schemas`() {
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
        assertTrue(schemas.engineSchema.userTypeNames == setOf("Query", "World", "Echo", "Issue", "JSON"))

        val testService = schemas.services.first { it.name == "test" }
        assertTrue(testService.definitionRegistry.typeNames == setOf("Query", "Echo", "World"))
        assertTrue(testService.underlyingSchema.userTypeNames == setOf("Query"))

        val issueService = schemas.services.first { it.name == "issue" }
        assertTrue(issueService.underlyingSchema.userTypeNames == setOf("Query", "Task"))
        assertTrue(issueService.definitionRegistry.typeNames == setOf("Query", "Issue"))
    }

    @Test
    fun `retains line numbers for string input`() {
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
            .captureSourceLocation(true)
            .captureAstDefinitions(true)
            .overallSchemas(overallSchema)
            .underlyingSchemas(underlyingSchema)
            .stubServiceExecution()
            .build()

        // then
        assertTrue(schemas.engineSchema.typeMap["Echo"]?.definition?.sourceLocation?.line == 4)
        assertTrue(schemas.engineSchema.typeMap["Issue"]?.definition?.sourceLocation?.line == 4)
        assertTrue(schemas.engineSchema.getField(echoCoordinates)?.definition?.sourceLocation?.line == 2)
        assertTrue(schemas.engineSchema.getField(worldCoordinates)?.definition?.sourceLocation?.line == 5)
        assertTrue(schemas.engineSchema.getField(issueIdCoordinates)?.definition?.sourceLocation?.line == 6)

        val testService = schemas.services.single { it.name == "test" }
        assertTrue(testService.underlyingSchema.typeMap["Echo"]?.definition?.sourceLocation?.line == 5)
        assertTrue(testService.underlyingSchema.getField(echoCoordinates)?.definition?.sourceLocation?.line == 2)
        assertTrue(testService.underlyingSchema.getField(worldCoordinates)?.definition?.sourceLocation?.line == 6)

        val issueService = schemas.services.single { it.name == "issue" }
        assertTrue(issueService.underlyingSchema.typeMap["Task"]?.definition?.sourceLocation?.line == 4)
    }

    @Test
    fun `drops source location if not asked for`() {
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
            .captureSourceLocation(false)
            .overallSchemas(overallSchema)
            .underlyingSchemas(underlyingSchema)
            .captureAstDefinitions(true)
            .stubServiceExecution()
            .build()

        // then
        assertTrue(schemas.engineSchema.typeMap["Echo"]?.definition?.sourceLocation == SourceLocation.EMPTY)
        assertTrue(schemas.engineSchema.typeMap["Issue"]?.definition?.sourceLocation == SourceLocation.EMPTY)
        assertTrue(schemas.engineSchema.getField(echoCoordinates)?.definition?.sourceLocation == SourceLocation.EMPTY)
        assertTrue(schemas.engineSchema.getField(worldCoordinates)?.definition?.sourceLocation == SourceLocation.EMPTY)
        assertTrue(schemas.engineSchema.getField(issueIdCoordinates)?.definition?.sourceLocation == SourceLocation.EMPTY)

        val testService = schemas.services.single { it.name == "test" }
        assertTrue(testService.underlyingSchema.typeMap["Echo"]?.definition?.sourceLocation == SourceLocation.EMPTY)
        assertTrue(testService.underlyingSchema.getField(echoCoordinates)?.definition?.sourceLocation == SourceLocation.EMPTY)
        assertTrue(testService.underlyingSchema.getField(worldCoordinates)?.definition?.sourceLocation == SourceLocation.EMPTY)

        val issueService = schemas.services.single { it.name == "issue" }
        assertTrue(issueService.underlyingSchema.typeMap["Task"]?.definition?.sourceLocation == SourceLocation.EMPTY)
    }

    @Test
    fun `drops definitions if not asked for`() {
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

        // when
        val schemas = NadelSchemas.newNadelSchemas()
            .captureSourceLocation(false)
            .overallSchemas(overallSchema)
            .underlyingSchemas(underlyingSchema)
            .stubServiceExecution()
            .build()

        // then
        assertTrue(schemas.engineSchema.typeMap["Echo"]!!.definition == null)
        assertTrue(schemas.engineSchema.typeMap["Issue"]!!.definition == null)

        val issueType = schemas.engineSchema.typeMap["Issue"] as GraphQLObjectType
        assertTrue(issueType.getField("id").definition == null)
    }
}
