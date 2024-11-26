package graphql.nadel.validation

import graphql.nadel.validation.util.assertSingleOfType
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelUnionValidationTest {
    @Test
    fun `no errors for valid schema`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "entities" to /* language=GraphQL */ """
                    type Query {
                        entity(id: ID!): Entity
                    }
                    union Entity = Human | Dog
                    type Human {
                        id: ID!
                    }
                    type Dog {
                        name: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "entities" to /* language=GraphQL */ """
                    type Query {
                        entity(id: ID!): Entity
                    }
                    union Entity = Human | Dog
                    type Human {
                        id: ID!
                    }
                    type Dog {
                        name: String
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `union in overall schema can have fewer members than underlying schema`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "entities" to /* language=GraphQL */ """
                    type Query {
                        entity(id: ID!): Entity
                    }
                    union Entity = Dog
                    type Human {
                        id: ID!
                    }
                    type Dog {
                        name: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "entities" to /* language=GraphQL */ """
                    type Query {
                        entity(id: ID!): Entity
                    }
                    union Entity = Human | Dog
                    type Human {
                        id: ID!
                    }
                    type Dog {
                        name: String
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `passes valid schema where union member was renamed`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "entities" to /* language=GraphQL */ """
                    type Query {
                        entity(id: ID!): Entity
                    }
                    union Entity = Dog
                    type Human @renamed(from: "Homo") {
                        id: ID!
                    }
                    type Dog {
                        name: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "entities" to /* language=GraphQL */ """
                    type Query {
                        entity(id: ID!): Entity
                    }
                    union Entity = Homo | Dog
                    type Homo {
                        id: ID!
                    }
                    type Dog {
                        name: String
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `fails if irrelevant union member was renamed to same name of valid union member`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "entities" to /* language=GraphQL */ """
                    type Query {
                        entity(id: ID!): Entity
                    }
                    union Entity = Human
                    type Human @renamed(from: "Issue") {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "entities" to /* language=GraphQL */ """
                    type Query {
                        entity(id: ID!): Entity
                    }
                    union Entity = Human | Dog
                    type Human {
                        id: ID!
                    }
                    type Dog {
                        name: String
                    }
                    type Issue {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())

        val error = errors.assertSingleOfType<NadelSchemaValidationError.UnionHasExtraType>()
        assertTrue(error.service.name == "entities")
        assertTrue(error.unionType.name == "Entity")
        assertTrue(error.extraType.name == "Human")
    }

    @Test
    fun `errors if union in overall schema declares members not in underlying schema union`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "entities" to /* language=GraphQL */ """
                    type Query {
                        entity(id: ID!): Entity
                    }
                    union Entity = Human | Dog | Tree
                    type Human {
                        id: ID!
                    }
                    type Dog {
                        name: String
                    }
                    type Tree {
                        location: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "entities" to /* language=GraphQL */ """
                    type Query {
                        entity(id: ID!): Entity
                    }
                    union Entity = Human | Dog
                    type Human {
                        id: ID!
                    }
                    type Dog {
                        name: String
                    }
                    type Tree {
                        location: String
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())

        val error = errors.assertSingleOfType<NadelSchemaValidationError.UnionHasExtraType>()
        assertTrue(error.service.name == "entities")
        assertTrue(error.unionType.name == "Entity")
        assertTrue(error.extraType.name == "Tree")
    }

    @Test
    fun `cannot add types not defined in service`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to /* language=GraphQL */ """
                    type Issue {
                        id: ID!
                    }
                """.trimIndent(),
                "entities" to /* language=GraphQL */ """
                    type Query {
                        entity(id: ID!): Entity
                    }
                    union Entity = Human | Dog | Issue
                    type Human {
                        id: ID!
                    }
                    type Dog {
                        name: String
                    }
                    type Tree {
                        location: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "issues" to /* language=GraphQL */ """
                    type Query {
                        echo: String
                    }
                    type Issue {
                        id: ID!
                    }
                """.trimIndent(),
                "entities" to /* language=GraphQL */ """
                    type Query {
                        entity(id: ID!): Entity
                    }
                    union Entity = Human | Dog
                    type Human {
                        id: ID!
                    }
                    type Dog {
                        name: String
                    }
                    type Tree {
                        location: String
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())

        val error = errors.assertSingleOfType<NadelSchemaValidationError.MissingUnderlyingType>()
        assertTrue(error.service.name == "entities")
        assertTrue(error.overallType.name == "Issue")
    }

    @Test
    fun `runs type validation on unions members`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to /* language=GraphQL */ """
                    type Issue {
                        id: ID!
                    }
                """.trimIndent(),
                "entities" to /* language=GraphQL */ """
                    type Query {
                        entity(id: ID!): Entity
                    }
                    union Entity = Human | Dog | Issue
                    type Human {
                        id: ID!
                    }
                    type Dog {
                        name: String
                    }
                    type Tree {
                        location: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "issues" to /* language=GraphQL */ """
                    type Query {
                        echo: String
                    }
                    type Issue {
                        id: ID!
                    }
                """.trimIndent(),
                "entities" to /* language=GraphQL */ """
                    type Query {
                        entity(id: ID!): Entity
                    }
                    union Entity = Human | Dog | Issue
                    type Human {
                        id: ID!
                    }
                    type Dog {
                        name: String
                    }
                    type Tree {
                        location: String
                    }
                    type Issue {
                        key: ID!
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())

        val error = errors.assertSingleOfType<NadelSchemaValidationError.MissingUnderlyingField>()
        assertTrue(error.service.name == "entities")
        assertTrue(error.parentType.overall.name == "Issue")
        assertTrue(error.overallField.name == "id")
    }
}
