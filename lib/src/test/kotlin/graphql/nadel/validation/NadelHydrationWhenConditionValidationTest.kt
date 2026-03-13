package graphql.nadel.validation

import graphql.nadel.validation.util.assertSingleOfType
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

private const val source = "$" + "source"
private const val argument = "$" + "argument"

class NadelHydrationWhenConditionValidationTest {
    @Test
    fun `list type field is acceptable if its the source field`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to """
                        type Query {
                            issue: JiraIssue
                        }
                        type JiraIssue @renamed(from: "Issue") {
                            id: ID!
                        }
                    """.trimIndent(),
                "users" to """
                        type Query {
                            users(id: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            type: String
                            collaborators: [User] @hydrated(
                                field: "users"
                                arguments: [
                                    {name: "id", value: "$source.collaboratorIds"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "type"
                                        predicate: { equals: "issue" }
                                    }
                                }
                            )
                        }
                    """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                            collaboratorIds: [ID!]
                            type: String!
                        }
                    """.trimIndent(),
                "users" to """
                        type Query {
                            users(id: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
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
    fun `matches predicate fails validation if it is invalid regex`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to """
                        type Query {
                            issue: JiraIssue
                        }
                        type JiraIssue @renamed(from: "Issue") {
                            id: ID!
                        }
                    """.trimIndent(),
                "users" to """
                        type Query {
                            users(id: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            type: String
                            collaborators: [User] @hydrated(
                                field: "users"
                                arguments: [
                                    {name: "id", value: "$source.collaboratorIds"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "type"
                                        predicate: { matches: "[a-z" }
                                    }
                                }
                            )
                        }
                    """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                            collaboratorIds: [ID!]
                            type: String!
                        }
                    """.trimIndent(),
                "users" to """
                        type Query {
                            users(id: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.map { it.message }.isNotEmpty())

        val error = errors.assertSingleOfType<NadelHydrationConditionInvalidRegexError>()
        assertTrue(error.virtualField.name == "collaborators")
        assertTrue(error.regexString == "[a-z")
    }
}
