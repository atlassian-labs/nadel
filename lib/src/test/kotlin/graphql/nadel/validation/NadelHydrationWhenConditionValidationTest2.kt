package graphql.nadel.validation

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

private const val source = "$" + "source"
private const val argument = "$" + "argument"

class NadelHydrationWhenConditionValidationTest2 {
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
                                service: "users"
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
}
