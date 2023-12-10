package graphql.nadel.validation

import kotlin.test.Test
import kotlin.test.assertTrue

private const val source = "$" + "source"
private const val argument = "$" + "argument"

class NadelHydrationValidationTest2 {
    @Test
    fun `error on mixing index hydration`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issue: JiraIssue
                    }
                    union Person = User | Account
                    type JiraIssue @renamed(from: "Issue") {
                        id: ID!
                        creator: Person
                        @hydrated(
                            service: "users"
                            field: "user"
                            arguments: [
                                {name: "id", value: "$source.creator"}
                            ]
                        )
                        @hydrated(
                            service: "users"
                            field: "account"
                            arguments: [
                                {name: "id", value: "$source.creator"}
                            ]
                            indexed: true
                        )
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        user(id: ID!): User
                        account(id: ID!): Account
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                    type Account {
                        id: ID!
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issue: Issue
                    }
                    type Issue {
                        id: ID!
                        creator: ID!
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        user(id: ID!): User
                        account(id: ID!): Account
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                    type Account {
                        id: ID!
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())
        assertTrue(errors.single() is NadelSchemaValidationError.MixedIndexHydration)
        assertTrue(errors.single().subject.name == "creator")
    }
}
