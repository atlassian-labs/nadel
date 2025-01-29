package graphql.nadel.validation.hydration

import graphql.nadel.validation.NadelAmbiguousUnionDefaultHydrationError
import graphql.nadel.validation.NadelDefaultHydrationIdArgumentNotFoundError
import graphql.nadel.validation.NadelDefaultHydrationIncompatibleBackingFieldTypeError
import graphql.nadel.validation.NadelValidationTestFixture
import graphql.nadel.validation.util.assertSingleOfType
import graphql.nadel.validation.validate
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelDefaultHydrationDefinitionValidationTest {
    @Test
    fun `passes valid default hydration`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                    }
                    type Comment @defaultHydration(field: "comments", idArgument: "ids", identifiedBy: "id") {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                    }
                    type Comment {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `fails if default hydration references non existent id argument`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                    }
                    type Comment @defaultHydration(field: "comments", idArgument: "identity", identifiedBy: "id") {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                    }
                    type Comment {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.map { it.message }.isNotEmpty())

        val error = errors.assertSingleOfType<NadelDefaultHydrationIdArgumentNotFoundError>()
        assertTrue(error.backingField.name == "comments")
        assertTrue(error.defaultHydration.idArgument == "identity")
    }

    @Test
    fun `fails if backing field does not return compatible type`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                        issues(ids: [ID!]!): [Issue]
                    }
                    type Comment @defaultHydration(field: "issues", idArgument: "ids", identifiedBy: "id") {
                        id: ID!
                    }
                    type Issue {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                        issues(ids: [ID!]!): [Issue]
                    }
                    type Comment {
                        id: ID!
                    }
                    type Issue {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.map { it.message }.isNotEmpty())

        val error = errors.assertSingleOfType<NadelDefaultHydrationIncompatibleBackingFieldTypeError>()
        assertTrue(error.type.overall.name == "Comment")
        assertTrue(error.backingField.name == "issues")
    }

    /**
     * This is a weird one. Technically it's a valid hydration. We allow it.
     *
     * There is validation elsewhere that says the `@idHydrated` field must return all implementations of the interface
     */
    @Test
    fun `passes if backing field returns interface that declaring type implements`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                        things(ids: [ID!]!): [Thing]
                    }
                    interface Thing {
                        id: ID!
                    }
                    type Comment implements Thing @defaultHydration(field: "things", idArgument: "ids", identifiedBy: "id") {
                        id: ID!
                    }
                    type Issue implements Thing {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                        things(ids: [ID!]!): [Thing]
                    }
                    interface Thing {
                        id: ID!
                    }
                    type Comment implements Thing {
                        id: ID!
                    }
                    type Issue implements Thing {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.map { it.message }.isEmpty())
    }

    /**
     * This is a weird one. Technically it's a valid hydration. We allow it.
     *
     * There is validation elsewhere that says the `@idHydrated` field must return all members of the union
     */
    @Test
    fun `passes if backing field returns union that the declaring type is part of`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                        things(ids: [ID!]!): [Thing]
                    }
                    union Thing = Comment | Issue
                    type Comment @defaultHydration(field: "things", idArgument: "ids", identifiedBy: "id") {
                        id: ID!
                    }
                    type Issue {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                        things(ids: [ID!]!): [Thing]
                    }
                    union Thing = Comment | Issue
                    type Comment {
                        id: ID!
                    }
                    type Issue {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `fails if multiple types are backed by same field and their configs are different`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                        users(ids: [ID!]!): [User]
                    }
                    type Comment {
                        id: ID!
                        commenterId: ID @hidden
                        commenter: Commenter @idHydrated(idField: "commenterId")
                    }
                    union Commenter = AtlassianAccountUser | CustomerUser | AppUser
                    interface User {
                        id: ID!
                    }
                    type AtlassianAccountUser implements User @defaultHydration(field: "users", idArgument: "ids") {
                        id: ID!
                    }
                    type CustomerUser implements User @defaultHydration(field: "users", idArgument: "ids", batchSize: 10) {
                        id: ID!
                    }
                    type AppUser implements User @defaultHydration(field: "users", idArgument: "ids") {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        comments(ids: [ID!]!): [Comment]
                        users(ids: [ID!]!): [User]
                    }
                    type Comment {
                        id: ID!
                        commenterId: ID
                    }
                    union Commenter = AtlassianAccountUser | CustomerUser | AppUser
                    interface User {
                        id: ID!
                    }
                    type AtlassianAccountUser implements User {
                        id: ID!
                    }
                    type CustomerUser implements User {
                        id: ID!
                    }
                    type AppUser implements User {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.map { it.message }.isNotEmpty())

        val error = errors.assertSingleOfType<NadelAmbiguousUnionDefaultHydrationError>()
        assertTrue(error.parentType.name == "Comment")
        assertTrue(error.virtualField.name == "commenter")
        assertTrue(error.backingField == listOf("users"))
    }
}
