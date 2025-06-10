package graphql.nadel.validation

import graphql.nadel.engine.util.singleOfType
import graphql.nadel.validation.NadelSchemaValidationError.CannotRenameHydratedField
import graphql.nadel.validation.NadelSchemaValidationError.RenameMustBeUsedExclusively
import graphql.nadel.validation.util.assertSingleOfType
import graphql.schema.GraphQLNamedType
import org.junit.jupiter.api.Test

private const val source = "$" + "source"
private const val argument = "$" + "argument"

class NadelHydrationValidationTest {
    @Test
    fun `passes if hydration is valid`() {
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

    @Test
    fun `fails when batch hydration with multiple $source args`() {
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
                                {name: "ids", value: "$source.creator"}
                                {name: "siteId", value: "$source.siteId"}
                            ]
                        )
                    }
                """.trimIndent(),
                "users" to """
                    type Query {
                        users(ids: [ID!], siteId: ID!): [User]
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
                        users(ids: [ID!], siteId: ID!): [User]
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
        assert(errors.single() is NadelBatchHydrationArgumentMultipleSourceFieldsError)
    }

    @Test
    fun `passes when batch hydration with a single $source arg and an $argument arg`() {
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
                        users(id: [ID!]!, siteId: ID!): [User]
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
                        users(id: [ID!]!, siteId: ID!): [User]
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

    @Test
    fun `fails if non-existent hydration reference field`() {
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
        errors.assertSingleOfType<NadelHydrationReferencesNonExistentBackingFieldError>()
    }

    @Test
    fun `fails if hydration backing field exists only in the underlying and not in the overall`() {
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
        val error = errors.assertSingleOfType<NadelHydrationReferencesNonExistentBackingFieldError>()
        assert(error.service.name == "issues")
        assert(error.virtualField.name == "creator")
        assert(error.parentType.overall.name == "Issue")
    }

    @Test
    fun `fails if hydrated field has rename`() {
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

        val error = errors.assertSingleOfType<RenameMustBeUsedExclusively>()
        assert(error.parentType.overall.name == "JiraIssue")
        assert(error.parentType.underlying.name == "Issue")
        assert(error.overallField.name == "creator")
    }

    @Test
    fun `can extend type with hydration`() {
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

    @Test
    fun `fails if hydrated field is not nullable`() {
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

        val error = errors.assertSingleOfType<NadelHydrationVirtualFieldMustBeNullableError>()
        assert(error.parentType.overall.name == "JiraIssue")
        assert(error.parentType.underlying.name == "Issue")
        assert(error.virtualField.name == "creator")
    }

    @Test
    fun `fails if hydration backing field does not exist`() {
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

        val error = errors.assertSingleOfType<NadelHydrationReferencesNonExistentBackingFieldError>()
        assert(error.parentType.overall.name == "Issue")
        assert(error.parentType.underlying.name == "Issue")
        assert(error.virtualField.name == "creator")
        assert(error.hydration.backingField == listOf("userById"))
        assert(error.virtualField == error.subject)
    }

    @Test
    fun `fails if hydration argument references non existent field`() {
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

        val error = errors.assertSingleOfType<NadelHydrationArgumentReferencesNonExistentFieldError>()
        assert(error.parentType.overall.name == "Issue")
        assert(error.parentType.underlying.name == "Issue")
        assert(error.virtualField.name == "creator")
        assert(error.hydration.backingField == listOf("user"))
        assert(error.argument.pathToField == listOf("creatorId"))
    }

    @Test
    fun `fails if hydration argument references non existent argument`() {
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

        val error = errors.assertSingleOfType<NadelHydrationArgumentReferencesNonExistentArgumentError>()
        assert(error.parentType.overall.name == "Issue")
        assert(error.parentType.underlying.name == "Issue")
        assert(error.virtualField.name == "creator")
        assert(error.hydration.backingField == listOf("user"))
        assert(error.argument.argumentName == "secrets")
    }

    @Test
    fun `fails if hydration argument references non existent remote argument`() {
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

        val error = errors.assertSingleOfType<NadelHydrationReferencesNonExistentBackingArgumentError>()
        assert(error.parentType.overall.name == "Issue")
        assert(error.parentType.underlying.name == "Issue")
        assert(error.virtualField.name == "creator")
        assert(error.hydration.backingField == listOf("user"))
        assert(error.argument == "someArg")
    }

    @Test
    fun `fails if hydration defines duplicated arguments`() {
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

        val error = errors.assertSingleOfType<NadelHydrationArgumentDuplicatedError>()
        assert(error.parentType.overall.name == "Issue")
        assert(error.parentType.underlying.name == "Issue")
        assert(error.virtualField.name == "creator")
        assert(error.hydration.backingField == listOf("user"))
        assert(error.duplicates.map { it.name }.toSet() == setOf("id"))
    }

    @Test
    fun `fails if hydration field has missing non-nullable arguments with underlying top level fields`() {
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

        val error = errors.assertSingleOfType<NadelHydrationMissingRequiredBackingFieldArgumentError>()
        assert(error.parentType.overall.name == "Issue")
        assert(error.parentType.underlying.name == "Issue")
        assert(error.virtualField.name == "creator")
        assert(error.missingBackingArgument.name == "other")
    }

    @Test
    fun `passes if hydration field has missing nullable arguments with underlying top level fields`() {
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

    @Test
    fun `checks the output type of the backing field against the output type of the hydrated field`() {
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

        val error = errors.assertSingleOfType<NadelHydrationIncompatibleOutputTypeError>()
        assert(error.parentType.overall.name == "Issue")
        assert(error.virtualField.name == "creator")
        assert(error.subject == error.virtualField)
        assert(error.incompatibleOutputType.name == "Account")
    }

    @Test
    fun `fails if one of the hydration return types is not in the union`() {
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
                            field: "externalUser"
                            arguments: [
                                {name: "id", value: "$source.creatorId"}
                            ]
                        )
                        @hydrated(
                            field: "user"
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
        val error = errors.singleOfType<NadelHydrationIncompatibleOutputTypeError>()
        assert(error.parentType.overall.name == "Issue")
        assert(error.backingField.name == "externalUser")
        assert((error.backingField.type as GraphQLNamedType).name == "ExternalUser")
        assert(error.incompatibleOutputType.name == "ExternalUser")
    }

    @Test
    fun `fails if backing output type does not implement interface`() {
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
        val error = errors.singleOfType<NadelHydrationIncompatibleOutputTypeError>()
        assert(error.parentType.overall.name == "Issue")
        assert(error.backingField.name == "externalUser")
        assert(error.incompatibleOutputType.name == "ExternalUser")
    }

    @Test
    fun `passes if backing output type implements the interface`() {
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

    @Test
    fun `passes if backing output type belongs in union`() {
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
                            field: "externalUser"
                            arguments: [
                                {name: "id", value: "$source.creatorId"}
                            ]
                        )
                        @hydrated(
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
