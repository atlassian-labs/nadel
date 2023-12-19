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

    @Test
    fun `prohibit multiple source input fields if they are list types`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    union ActivityContent = User | Issue
                    type Activity {
                        id: ID!
                        data: [ActivityContent]
                        @hydrated(
                            service: "users"
                            field: "usersByIds"
                            arguments: [
                                {name: "ids", value: "$source.userIds"}
                            ]
                        )
                        @hydrated(
                            service: "issues"
                            field: "issuesByIds"
                            arguments: [
                                {name: "ids", value: "$source.issueIds"}
                            ]
                        )
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        usersByIds(ids: [ID]!): [User]
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issuesByIds(ids: [ID]!): [Issue]
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                        userIds: [ID]
                        issueIds: [ID]
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        usersByIds(ids: [ID]!): [User]
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                    type Account {
                        id: ID!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issuesByIds(ids: [ID]!): [Issue]
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())
        assertTrue(errors.single() is NadelSchemaValidationError.MultipleHydrationSourceInputFields)
        assertTrue(errors.single().subject.name == "data")
    }

    @Test
    fun `prohibit multiple source fields where list not the leaf`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    union ActivityContent = User | Issue
                    type Activity {
                        id: ID!
                        data: [ActivityContent]
                        @hydrated(
                            service: "users"
                            field: "usersByIds"
                            arguments: [
                                {name: "ids", value: "$source.contexts.userId"}
                            ]
                        )
                        @hydrated(
                            service: "issues"
                            field: "issuesByIds"
                            arguments: [
                                {name: "ids", value: "$source.contexts.issueId"}
                            ]
                        )
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        usersByIds(ids: [ID]!): [User]
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issuesByIds(ids: [ID]!): [Issue]
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type ActivityContext {
                        issueId: ID
                        userId: ID
                    }
                    type Activity {
                        id: ID!
                        contexts: [ActivityContext]
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        usersByIds(ids: [ID]!): [User]
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                    type Account {
                        id: ID!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issuesByIds(ids: [ID]!): [Issue]
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())
        assertTrue(errors.single() is NadelSchemaValidationError.MultipleHydrationSourceInputFields)
        assertTrue(errors.single().subject.name == "data")
    }

    @Test
    fun `prohibit mixing list and non-list source input fields`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    union ActivityContent = User | Issue
                    type Activity {
                        id: ID!
                        data: [ActivityContent]
                        @hydrated(
                            service: "users"
                            field: "usersByIds"
                            arguments: [
                                {name: "ids", value: "$source.userIds"}
                            ]
                        )
                        @hydrated(
                            service: "issues"
                            field: "issuesByIds"
                            arguments: [
                                {name: "ids", value: "$source.issueId"}
                            ]
                        )
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        usersByIds(ids: [ID]!): [User]
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issuesByIds(ids: [ID]!): [Issue]
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                        userIds: [ID]
                        issueId: ID
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        usersByIds(ids: [ID]!): [User]
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                    type Account {
                        id: ID!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issuesByIds(ids: [ID]!): [Issue]
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isNotEmpty())
        assertTrue(errors.single() is NadelSchemaValidationError.MultipleHydrationSourceInputFields)
        assertTrue(errors.single().subject.name == "data")
    }

    @Test
    fun `permit multiple source fields if source input field is not list type`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    union ActivityContent = User | Issue
                    type Activity {
                        id: ID!
                        data: ActivityContent
                        @hydrated(
                            service: "users"
                            field: "usersByIds"
                            arguments: [
                                {name: "ids", value: "$source.userId"}
                            ]
                        )
                        @hydrated(
                            service: "issues"
                            field: "issuesByIds"
                            arguments: [
                                {name: "ids", value: "$source.issueId"}
                            ]
                        )
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        usersByIds(ids: [ID]!): [User]
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issuesByIds(ids: [ID]!): [Issue]
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                        userId: ID
                        issueId: ID
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        usersByIds(ids: [ID]!): [User]
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                    type Account {
                        id: ID!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issuesByIds(ids: [ID]!): [Issue]
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `permit multiple source fields non batched hydration`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    union ActivityContent = User | Issue
                    type Activity {
                        id: ID!
                        data: ActivityContent
                        @hydrated(
                            service: "users"
                            field: "userById"
                            arguments: [
                                {name: "id", value: "$source.userId"}
                            ]
                        )
                        @hydrated(
                            service: "issues"
                            field: "issueById"
                            arguments: [
                                {name: "id", value: "$source.issueId"}
                            ]
                        )
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        userById(id: ID): User
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issueById(id: ID): Issue
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "activity" to /* language=GraphQL*/ """
                    type Query {
                        myActivity: [Activity]
                    }
                    type Activity {
                        id: ID!
                        userId: ID
                        issueId: ID
                    }
                """.trimIndent(),
                "users" to /* language=GraphQL*/ """
                    type Query {
                        userById(id: ID): User
                    }
                    type User {
                        id: ID!
                        name: String!
                    }
                    type Account {
                        id: ID!
                    }
                """.trimIndent(),
                "issues" to /* language=GraphQL*/ """
                    type Query {
                        issueById(id: ID): Issue
                    }
                    type Issue {
                        id: ID!
                        key: String
                    }
                """.trimIndent(),
            ),
        )

        val errors = validate(fixture)
        assertTrue(errors.map { it.message }.isEmpty())
    }
}
