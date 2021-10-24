package graphql.nadel.validation

import graphql.nadel.enginekt.util.singleOfType
import graphql.nadel.validation.NadelSchemaValidationError.CannotRenameHydratedField
import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedHydrationArgument
import graphql.nadel.validation.NadelSchemaValidationError.HydrationFieldMustBeNullable
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationActorField
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationActorFieldArgument
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationActorService
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationArgumentValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationFieldValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingField
import io.kotest.core.spec.style.DescribeSpec

private const val source = "$" + "source"
private const val argument = "$" + "argument"

class NadelHydrationValidationTest : DescribeSpec({
    describe("validate") {
        it("passes if hydration is valid") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: JiraIssue
                        }
                        type JiraIssue @renamed(from: "Issue") {
                            id: ID!
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
                            )
                        }
                    """.trimIndent(),
                    "users" to """
                        type User {
                            id: ID!
                            name: String!
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

        it("fails if hydrated field has rename") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: JiraIssue
                        }
                        type JiraIssue @renamed(from: "Issue") {
                            id: ID!
                            # Rename doesn't make sense but we're testing that the directives cannot coexist
                            creator: User @renamed(from: "id") @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
                            )
                        }
                    """.trimIndent(),
                    "users" to """
                        type User {
                            id: ID!
                            name: String!
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

            val error = errors.singleOfType<CannotRenameHydratedField>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.parentType.underlying.name == "Issue")
            assert(error.overallField.name == "creator")
        }

        it("can extend type with hydration") {
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

        it("fails if hydration actor service does not exist") {
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
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            creator: User @hydrated(
                                service: "userService"
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

            val error = errors.singleOfType<MissingHydrationActorService>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.parentType.underlying.name == "Issue")
            assert(error.overallField.name == "creator")
            assert(error.subject == error.overallField)
            assert(error.hydration.serviceName == "userService")
        }

        it("fails if hydrated field is not null") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: JiraIssue
                        }
                        type JiraIssue @renamed(from: "Issue") {
                            creator: User! @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
                            )
                        }
                    """.trimIndent(),
                    "users" to """
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
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

            val error = errors.singleOfType<HydrationFieldMustBeNullable>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.parentType.underlying.name == "Issue")
            assert(error.overallField.name == "creator")
        }

        it("fails if hydration actor field does not exist") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            creator: User @hydrated(
                                service: "users"
                                field: "userById"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
                            )
                        }
                    """.trimIndent(),
                    "users" to """
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
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

            val error = errors.singleOfType<MissingHydrationActorField>()
            assert(error.parentType.overall.name == "Issue")
            assert(error.parentType.underlying.name == "Issue")
            assert(error.overallField.name == "creator")
            assert(error.hydration.topLevelField == "userById")
            assert(error.overallField == error.subject)
        }

        it("fails if hydration argument references non existent field") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creatorId"}
                                ]
                            )
                        }
                    """.trimIndent(),
                    "users" to """
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
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

            val error = errors.singleOfType<MissingHydrationFieldValueSource>()
            assert(error.parentType.overall.name == "Issue")
            assert(error.parentType.underlying.name == "Issue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArgSource.path == listOf("creatorId"))
            assert(error.remoteArgSource.name == null)
        }

        it("fails if hydration argument references non existent argument") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                        }
                    """.trimIndent(),
                    "users" to """
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type Issue {
                            creator(someArg: Boolean): User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                    {name: "secrets", value: "$argument.secrets"}
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
                            user(id: ID!, secrets: Boolean = false): User
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

            val error = errors.singleOfType<MissingHydrationArgumentValueSource>()
            assert(error.parentType.overall.name == "Issue")
            assert(error.parentType.underlying.name == "Issue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArgSource.name == "secrets")
            assert(error.remoteArgSource.path == null)
        }

        it("fails if hydration argument references non existent remote argument") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                        }
                    """.trimIndent(),
                    "users" to """
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type Issue {
                            creator(someArg: Boolean): User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                    {name: "someArg", value: "$argument.someArg"}
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

            val error = errors.singleOfType<MissingHydrationActorFieldArgument>()
            assert(error.parentType.overall.name == "Issue")
            assert(error.parentType.underlying.name == "Issue")
            assert(error.overallField.name == "creator")
            assert(error.argument == "someArg")
        }

        it("fails if hydration defines duplicated arguments") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                        }
                    """.trimIndent(),
                    "users" to """
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type Issue {
                            creator(someArg: ID!, other: Boolean): User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                    {name: "id", value: "$argument.someArg"}
                                    {name: "other", value: "$argument.other"}
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
                            user(id: ID!, other: Boolean): User
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

            val error = errors.singleOfType<DuplicatedHydrationArgument>()
            assert(error.parentType.overall.name == "Issue")
            assert(error.parentType.underlying.name == "Issue")
            assert(error.overallField.name == "creator")
            assert(error.duplicates.map { it.name }.toSet() == setOf("id"))
        }

        it("checks the output type of the actor field against the output type of the hydrated field") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                        }
                    """.trimIndent(),
                    "users" to """
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type Issue {
                            creator(someArg: ID!, other: Boolean): User @hydrated(
                                service: "accounts"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                    {name: "id", value: "$argument.someArg"}
                                    {name: "other", value: "$argument.other"}
                                ]
                            )
                        }
                    """.trimIndent(),
                    "accounts" to """
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
                            user(id: ID!, other: Boolean): User
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                    "accounts" to """
                        type Query {
                            user(id: ID!, other: Boolean): Account 
                        }
                        type Account {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.singleOfType<MissingUnderlyingField>()
            assert(error.parentType.overall.name == "User")
            assert(error.parentType.underlying.name == "Account")
            assert(error.overallField.name == "name")
            assert(error.subject == error.overallField)
        }
    }
})

