package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleArgumentTypeForActorField
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldOutputType
import graphql.nadel.validation.util.assertSingleOfType
import io.kotest.core.spec.style.DescribeSpec

private const val source = "$" + "source"
private const val argument = "$" + "argument"

class NadelHydrationArgumentValidationTest : DescribeSpec({
    describe("Hydration arg validation") {

        it("fails if the source field type doesnt match the actor field argument type") {
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
                            user(id: Boolean): User
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
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
                            creator: ID!
                        }
                    """.trimIndent(),
                            "users" to """
                        type Query {
                            user(id: Boolean): User
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                    ),
            )
            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<IncompatibleArgumentTypeForActorField>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.subject == error.overallField)
        }

        it("String type should be assignable to ID") {
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
                            user(id: ID!): User
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
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
                            creator: String!
                        }
                    """.trimIndent(),
                            "users" to """
                        type Query {
                            user(id: ID!): User
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                    ),
            )
            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        it("ID should not be assignable to String") {
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
                            user(id: String!): User
                        }
                        type User {
                            id: String!
                            name: String!
                        }
                        extend type JiraIssue {
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
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
                            id: String!
                            creator: ID!
                        }
                    """.trimIndent(),
                            "users" to """
                        type Query {
                            user(id: ID!): User
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                    ),
            )
            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<IncompatibleArgumentTypeForActorField>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.subject == error.overallField)
        }

        it("non-null should be assignable to nullable") {
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
                            user(id: ID!): User
                        }
                        type User {
                            id: ID!
                            name: String
                        }
                        extend type JiraIssue {
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
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
                            creator: ID
                        }
                    """.trimIndent(),
                            "users" to """
                        type Query {
                            user(id: ID!): User
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                    ),
            )
            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        it("nullable should not be assignable to non-null") {
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
                            user(id: String!): User
                        }
                        type User {
                            id: String!
                            name: String!
                        }
                        extend type JiraIssue {
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
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
                            id: String!
                            creator: String
                        }
                    """.trimIndent(),
                            "users" to """
                        type Query {
                            user(id: String!): User
                        }
                        type User {
                            id: String!
                            name: String!
                        }
                    """.trimIndent(),
                    ),
            )
            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<IncompatibleFieldOutputType>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "id")
            assert(error.subject == error.overallField)
        }

        it("testing batch") {
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
                            users(ids: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            creator: User @hydrated(
                                service: "users"
                                field: "users"
                                arguments: [
                                    {name: "ids", value: "$source.creators"}
                                ]
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
                            creators: [ID!]!
                        }
                    """.trimIndent(),
                            "users" to """
                        type Query {
                            users(ids: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                    ),
            )
            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        it("testing batch - ID should not be assignable to String") {
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
                            users(ids: [String!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            creator: User @hydrated(
                                service: "users"
                                field: "users"
                                arguments: [
                                    {name: "ids", value: "$source.creators"}
                                ]
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
                            creators: [ID!]!
                        }
                    """.trimIndent(),
                            "users" to """
                        type Query {
                            users(ids: [String!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                    ),
            )
            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<IncompatibleArgumentTypeForActorField>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.subject == error.overallField)
        }

        it("testing batch - nullable should not be assignable to non-null") {
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
                            users(ids: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            creator: User @hydrated(
                                service: "users"
                                field: "users"
                                arguments: [
                                    {name: "ids", value: "$source.creators"}
                                ]
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
                            creators: [ID]!
                        }
                    """.trimIndent(),
                            "users" to """
                        type Query {
                            users(ids: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                    ),
            )
            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<IncompatibleArgumentTypeForActorField>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.subject == error.overallField)
        }

        it("testing batch - compatible list of lists passes validation") {
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
                            users(ids: [[ID!]!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            creator: User @hydrated(
                                service: "users"
                                field: "users"
                                arguments: [
                                    {name: "ids", value: "$source.creators"}
                                ]
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
                            creators: [[String!]!]!
                        }
                    """.trimIndent(),
                            "users" to """
                        type Query {
                            users(ids: [[ID!]!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                    ),
            )
            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        it("testing batch - incompatible list of lists fails validation") {
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
                            users(ids: [[String!]!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            creator: User @hydrated(
                                service: "users"
                                field: "users"
                                arguments: [
                                    {name: "ids", value: "$source.creators"}
                                ]
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
                            creators: [[ID!]!]!
                        }
                    """.trimIndent(),
                            "users" to """
                        type Query {
                            users(ids: [[String!]!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                    ),
            )
            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<IncompatibleArgumentTypeForActorField>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.subject == error.overallField)
        }

        it("input object - allows compatible input objects") {}

        it("input object - allows compatible input objects") {}
    }
})