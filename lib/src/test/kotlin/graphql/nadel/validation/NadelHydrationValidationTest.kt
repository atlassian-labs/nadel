package graphql.nadel.validation

import graphql.nadel.engine.util.singleOfType
import graphql.nadel.validation.NadelSchemaValidationError.CannotRenameHydratedField
import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedHydrationArgument
import graphql.nadel.validation.NadelSchemaValidationError.HydrationFieldMustBeNullable
import graphql.nadel.validation.NadelSchemaValidationError.HydrationIncompatibleOutputType
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationActorField
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationArgumentValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationFieldValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MissingRequiredHydrationActorFieldArgument
import graphql.nadel.validation.NadelSchemaValidationError.NonExistentHydrationActorFieldArgument
import graphql.nadel.validation.util.assertSingleOfType
import graphql.schema.GraphQLNamedType
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
                        type Query {
                            user(id: ID!): User
                        }
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

        it("fails when batch hydration with multiple \$source args") {
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
                                field: "users"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                    {name: "siteId", value: "$source.siteId"}
                                ]
                            )
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            users(id: ID!, siteId: ID!): [User]
                        }
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
                            siteId: ID!
                            creator: ID!
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            users(id: ID!, siteId: ID!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.size == 1)
            assert(errors.single() is NadelSchemaValidationError.MultipleSourceArgsInBatchHydration)
            assert(errors.single().message == "Multiple \$source.xxx arguments are not supported for batch hydration. Field: JiraIssue.creator")
        }

        it("fails when batch hydration with no \$source args") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: JiraIssue
                        }
                        type JiraIssue @renamed(from: "Issue") {
                            id: ID!
                            creator(siteId: ID!): User @hydrated(
                                service: "users"
                                field: "users"
                                arguments: [
                                    {name: "id", value: "creatorId123"}
                                    {name: "siteId", value: "$argument.siteId"}
                                ]
                            )
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            users(id: ID!, siteId: ID!): [User]
                        }
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
                            users(id: ID!, siteId: ID!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.size == 1)
            assert(errors.single() is NadelSchemaValidationError.NoSourceArgsInBatchHydration)
            assert(errors.single().message == "No \$source.xxx arguments for batch hydration. Field: JiraIssue.creator")
        }

        it("passes when batch hydration with a single \$source arg and an \$argument arg") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: JiraIssue
                        }
                        type JiraIssue @renamed(from: "Issue") {
                            id: ID!
                            creator(siteId: ID!): User @hydrated(
                                service: "users"
                                field: "users"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                    {name: "siteId", value: "$argument.siteId"}
                                ]
                            )
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            users(id: ID!, siteId: ID!): [User]
                        }
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
                            users(id: ID!, siteId: ID!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.isEmpty())
        }

        it("fails if non-existent hydration reference field") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                            creator: User @hydrated(
                                service: "users"
                                field: "fakeField.user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
                            )
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
            errors.assertSingleOfType<MissingHydrationActorField>()
        }

        it("fails if hydration actor field exists only in the underlying and not in the overall") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
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

            assert(errors.size == 1)
            val error = errors.assertSingleOfType<MissingHydrationActorField>()
            assert(error.service.name == "issues")
            assert(error.overallField.name == "creator")
            assert(error.parentType.overall.name == "Issue")
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
                        type Query {
                            user(id: ID!): User
                        }
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

            val error = errors.assertSingleOfType<CannotRenameHydratedField>()
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

        it("fails if hydrated field is not nullable") {
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
                        type Query {
                            user(id: ID!): User
                        }
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

            val error = errors.assertSingleOfType<HydrationFieldMustBeNullable>()
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

            val error = errors.assertSingleOfType<MissingHydrationActorField>()
            assert(error.parentType.overall.name == "Issue")
            assert(error.parentType.underlying.name == "Issue")
            assert(error.overallField.name == "creator")
            assert(error.hydration.backingField == listOf("userById"))
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
                        type Query {
                            user(id: ID!): User
                        }
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

            val error = errors.assertSingleOfType<MissingHydrationFieldValueSource>()
            assert(error.parentType.overall.name == "Issue")
            assert(error.parentType.underlying.name == "Issue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArgSource.pathToField == listOf("creatorId"))
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
                        type Query {
                            user(id: ID!, secrets: Boolean = false): User
                        }
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

            val error = errors.assertSingleOfType<MissingHydrationArgumentValueSource>()
            assert(error.parentType.overall.name == "Issue")
            assert(error.parentType.underlying.name == "Issue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArgSource.argumentName == "secrets")
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
                        type Query {
                            user(id: ID!): User
                        }
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

            val error = errors.assertSingleOfType<NonExistentHydrationActorFieldArgument>()
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
                        type Query {
                            user(id: ID!, other: Boolean): User
                        }
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

            val error = errors.assertSingleOfType<DuplicatedHydrationArgument>()
            assert(error.parentType.overall.name == "Issue")
            assert(error.parentType.underlying.name == "Issue")
            assert(error.overallField.name == "creator")
            assert(error.duplicates.map { it.name }.toSet() == setOf("id"))
        }

        it("fails if hydration field has missing non-nullable arguments with underlying top level fields") {
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
                        type Query {
                            user(id: ID!, other: Boolean!): User
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type Issue {
                            creator(someArg: ID!, other: Boolean): User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$argument.creator"}
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
                            user(id: ID!, other: Boolean!): User
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

            val error = errors.assertSingleOfType<MissingRequiredHydrationActorFieldArgument>()
            assert(error.parentType.overall.name == "Issue")
            assert(error.parentType.underlying.name == "Issue")
            assert(error.overallField.name == "creator")
            assert(error.argument == "other")
        }

        it("passes if hydration field has missing nullable arguments with underlying top level fields") {
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
                        type Query {
                            user(id: ID!, other: Boolean): User
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type Issue {
                            creator(someArg: ID!, other: Boolean): User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$argument.someArg"}
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
            assert(errors.map { it.message }.isEmpty())
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
                                    {name: "other", value: "$argument.other"}
                                ]
                            )
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

            val error = errors.assertSingleOfType<HydrationIncompatibleOutputType>()
            assert(error.parentType.overall.name == "Issue")
            assert(error.overallField.name == "creator")
            assert(error.subject == error.overallField)
            assert(error.incompatibleOutputType.name == "Account")
        }

        it("fails if one of the hydration return types is not in the union") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                            creator: AbstractUser
                            @hydrated(
                                service: "users"
                                field: "externalUser"
                                arguments: [
                                    {name: "id", value: "$source.creatorId"}
                                ]
                            )
                        }
                        union AbstractUser = User
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            user(id: ID!): User
                            externalUser(id: ID!): ExternalUser
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        type ExternalUser {
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
                            creatorId: ID!
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            user(id: ID!): User
                            externalUser(id: ID!): ExternalUser
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        type ExternalUser {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.isNotEmpty())
            val error = errors.singleOfType<HydrationIncompatibleOutputType>()
            assert(error.parentType.overall.name == "Issue")
            assert(error.actorField.name == "externalUser")
            assert((error.actorField.type as GraphQLNamedType).name == "ExternalUser")
            assert(error.incompatibleOutputType.name == "ExternalUser")
        }

        it("fails if actor output type does not implement interface") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                            creator: AbstractUser
                            @hydrated(
                                service: "users"
                                field: "externalUser"
                                arguments: [
                                    {name: "id", value: "$source.creatorId"}
                                ]
                            )
                        }
                        interface AbstractUser {
                            id: ID!
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            user(id: ID!): User
                            externalUser(id: ID!): ExternalUser
                        }
                        type User implements AbstractUser {
                            id: ID!
                            name: String!
                        }
                        type ExternalUser {
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
                            creatorId: ID!
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            user(id: ID!): User
                            externalUser(id: ID!): ExternalUser
                        }
                        type User implements AbstractUser {
                            id: ID!
                            name: String!
                        }
                        type ExternalUser {
                            id: ID!
                            name: String!
                        }
                        interface AbstractUser {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.isNotEmpty())
            val error = errors.singleOfType<HydrationIncompatibleOutputType>()
            assert(error.parentType.overall.name == "Issue")
            assert(error.actorField.name == "externalUser")
            assert(error.incompatibleOutputType.name == "ExternalUser")
        }

        it("passes if actor output type implements the interface") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                            creator: AbstractUser
                            @hydrated(
                                service: "users"
                                field: "externalUser"
                                arguments: [
                                    {name: "id", value: "$source.creatorId"}
                                ]
                            )
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            user(id: ID!): User
                            externalUser(id: ID!): ExternalUser
                        }
                        type User implements AbstractUser {
                            id: ID!
                            name: String!
                        }
                        type ExternalUser implements AbstractUser {
                            id: ID!
                            name: String!
                        }
                        interface AbstractUser {
                            id: ID!
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
                            creatorId: ID!
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            user(id: ID!): User
                            externalUser(id: ID!): ExternalUser
                        }
                        type User implements AbstractUser {
                            id: ID!
                            name: String!
                        }
                        type ExternalUser implements AbstractUser {
                            id: ID!
                            name: String!
                        }
                        interface AbstractUser {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        it("passes if actor output type belongs in union") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                            creator: AbstractUser
                            @hydrated(
                                service: "users"
                                field: "externalUser"
                                arguments: [
                                    {name: "id", value: "$source.creatorId"}
                                ]
                            )
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            user(id: ID!): User
                            externalUser(id: ID!): ExternalUser
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        type ExternalUser {
                            id: ID!
                            name: String!
                        }
                        union AbstractUser = User | ExternalUser
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "issues" to """
                        type Query {
                            issue: Issue
                        }
                        type Issue {
                            id: ID!
                            creatorId: ID!
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            user(id: ID!): User
                            externalUser(id: ID!): ExternalUser
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        type ExternalUser {
                            id: ID!
                            name: String!
                        }
                        union AbstractUser = User | ExternalUser
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }
    }
})
