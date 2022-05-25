package graphql.nadel.validation

import graphql.nadel.engine.util.singleOfType
import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedHydrationArgument
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleHydrationArgumentType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleHydrationInputObjectArgumentType
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationArgumentValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationFieldValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationInputObjectArgumentValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MissingRequiredHydrationActorFieldArgument
import graphql.nadel.validation.NadelSchemaValidationError.NonExistentHydrationActorFieldArgument
import graphql.nadel.validation.util.assertSingleOfType
import graphql.schema.GraphQLTypeUtil
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData

private const val source = "$" + "source"
private const val argument = "$" + "argument"

class NadelHydrationArgumentValidationTest : DescribeSpec({
    describe("validate") {
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
            assert(error.remoteArgSource.argumentName == null)
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
            assert(error.remoteArgSource.pathToField == null)
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

        it("fails if hydration argument source type is mismatch with actor field input arguments") {
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
                                user(id: Int!): User
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
                                user(id: Int!): User
                            }
                            type User {
                                id: ID!
                                name: String!
                            }
                        """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.isNotEmpty())
            val error = errors.singleOfType<IncompatibleHydrationArgumentType>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "id")
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "ID!")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "Int!")
        }

        it("fails if hydration argument types are mismatch with actor field input arguments") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                            type Query {
                                issue: JiraIssue
                            }
                            type JiraIssue @renamed(from: "Issue") {
                                id: ID!
                                creator(someArg: ID!): User @hydrated(
                                    service: "users"
                                    field: "user"
                                    arguments: [
                                        {name: "someArg", value: "$argument.someArg"}
                                    ]
                                )
                            }
                        """.trimIndent(),
                    "users" to """
                            type Query {
                                user(someArg: Int!): User
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
                                user(someArg: Int!): User
                            }
                            type User {
                                id: ID!
                                name: String!
                            }
                        """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.isNotEmpty())
            val error = errors.singleOfType<IncompatibleHydrationArgumentType>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "someArg")
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "ID!")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "Int!")
        }


        it("passes if hydration argument source types are matching with batch hydration") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                            type Query {
                                issue: JiraIssue
                            }
                            type JiraIssue @renamed(from: "Issue") {
                                id: ID!
                                creators: [User] @hydrated(
                                    service: "users"
                                    field: "usersById"
                                    arguments: [
                                        {name: "id", value: "$source.creators.id"}
                                    ]
                                    identifiedBy: "id"
                                )
                            }
                        """.trimIndent(),
                    "users" to """
                            type Query {
                                usersById(id: [ID]): [User]
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
                                creators: [CreatorRef]
                            }
                            type CreatorRef {
                                id: ID!
                            }
                        """.trimIndent(),
                    "users" to """
                            type Query {
                                usersById(id: [ID]): [User]
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

        it("passes if hydration argument source types are matching with batch hydration with list input") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                            type Query {
                                issue: JiraIssue
                            }
                            type JiraIssue @renamed(from: "Issue") {
                                id: ID!
                                creators: [User] @hydrated(
                                    service: "users"
                                    field: "userById"
                                    arguments: [
                                        {name: "id", value: "$source.creators"}
                                    ]
                                    identifiedBy: "id"
                                )
                            }
                        """.trimIndent(),
                    "users" to """
                            type Query {
                                userById(id: ID): User
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
                                creators: [ID]
                            }
                        """.trimIndent(),
                    "users" to """
                            type Query {
                                userById(id: ID): User
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

        it("checks if there is a missing hydration source type when the hydration input type is an object type") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                            type Query {
                                issue: JiraIssue
                            }
                            type JiraIssue @renamed(from: "Issue") {
                                id: ID!
                                creators: [User] @hydrated(
                                    service: "users"
                                    field: "users"
                                    arguments: [
                                        {name: "id", value: "$source.creators"}
                                    ]
                                    inputIdentifiedBy: [
                                        {sourceId: "creators.userId" resultId: "id"}
                                        {sourceId: "creators.site" resultId: "siteId"}
                                    ]
                                )
                            }
                        """.trimIndent(),
                    "users" to """
                            type Query {
                                users(id: [UserInput]): [User]
                            }
                            input UserInput {
                                userId: ID
                                site: String!
                            }
                            type User {
                                id: ID!
                                name: String
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
                                creators: [UserRef]
                            }
                            type UserRef {
                                userId: ID
                            }
                        """.trimIndent(),
                    "users" to """
                            type Query {
                                users(id: [UserInput]): [User]
                            }
                            input UserInput {
                                userId: ID
                                site: String!
                            }
                            type User {
                                id: ID!
                                name: String!
                                siteId: ID
                            }
                        """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.isNotEmpty())
            val error = errors.singleOfType<MissingHydrationInputObjectArgumentValueSource>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creators")
            assert(error.remoteArg.name == "id")
            assert(error.actorFieldName == "site")
        }

        it("checks when hydration argument source type is object type") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                            type Query {
                                issue: JiraIssue
                            }
                            type JiraIssue @renamed(from: "Issue") {
                                id: ID!
                                creators: [User] @hydrated(
                                    service: "users"
                                    field: "users"
                                    arguments: [
                                        {name: "id", value: "$source.creators"}
                                    ]
                                    inputIdentifiedBy: [
                                        {sourceId: "creators.userId" resultId: "id"}
                                        {sourceId: "creators.site" resultId: "siteId"}
                                    ]
                                )
                            }
                        """.trimIndent(),
                    "users" to """
                            type Query {
                                users(id: [UserInput]): [User]
                            }
                            input UserInput {
                                userId: ID
                                site: String!
                            }
                            type User {
                                id: ID!
                                name: String
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
                                creators: [UserRef]
                            }
                            type UserRef {
                                userId: ID
                                site: Int!
                            }
                        """.trimIndent(),
                    "users" to """
                            type Query {
                                users(id: [UserInput]): [User]
                            }
                            input UserInput {
                                userId: ID
                                site: String!
                            }
                            type User {
                                id: ID!
                                name: String!
                                siteId: ID
                            }
                        """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.isNotEmpty())
            val error = errors.singleOfType<IncompatibleHydrationInputObjectArgumentType>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creators")
            assert(error.remoteArg.name == "id")
            assert(error.actorFieldName == "site")
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "Int!")
            assert(GraphQLTypeUtil.simplePrint(error.actorFieldType) == "String!")
        }

        it("checks if there is a missing hydration argument source type when the hydration input is an input object type") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                            type Query {
                                issue: JiraIssue
                            }
                            type JiraIssue @renamed(from: "Issue") {
                                id: ID!
                                creators(someArg: [CreatorInput]): [User] @hydrated(
                                    service: "users"
                                    field: "users"
                                    arguments: [
                                        {name: "id", value: "$argument.someArg"}
                                    ]
                                    inputIdentifiedBy: [
                                        {sourceId: "creators.userId" resultId: "id"}
                                        {sourceId: "creators.site" resultId: "siteId"}
                                    ]
                                )
                            }
                            input CreatorInput {
                                userId: ID
                            }
                        """.trimIndent(),
                    "users" to """
                            type Query {
                                users(id: [UserInput]): [User]
                            }
                            input UserInput {
                                userId: ID
                                site: String!
                            }
                            type User {
                                id: ID!
                                name: String
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
                            }
                        """.trimIndent(),
                    "users" to """
                            type Query {
                                users(id: [UserInput]): [User]
                            }
                            input UserInput {
                                userId: ID
                                site: String!
                            }
                            type User {
                                id: ID!
                                name: String!
                                siteId: ID
                            }
                        """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.isNotEmpty())
            val error = errors.singleOfType<MissingHydrationInputObjectArgumentValueSource>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creators")
            assert(error.remoteArg.name == "id")
            assert(error.actorFieldName == "site")
        }

        it("checks if there is a mismatched hydration argument source type when the hydration input is an input object type") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "issues" to """
                            type Query {
                                issue: JiraIssue
                            }
                            type JiraIssue @renamed(from: "Issue") {
                                id: ID!
                                creators(someArg: [CreatorInput]): [User] @hydrated(
                                    service: "users"
                                    field: "users"
                                    arguments: [
                                        {name: "id", value: "$argument.someArg"}
                                    ]
                                    inputIdentifiedBy: [
                                        {sourceId: "creators.userId" resultId: "id"}
                                        {sourceId: "creators.site" resultId: "siteId"}
                                    ]
                                )
                            }
                            input CreatorInput {
                                userId: ID
                                site: Int!
                            }
                        """.trimIndent(),
                    "users" to """
                            type Query {
                                users(id: [UserInput]): [User]
                            }
                            input UserInput {
                                userId: ID
                                site: String!
                            }
                            type User {
                                id: ID!
                                name: String
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
                            }
                        """.trimIndent(),
                    "users" to """
                            type Query {
                                users(id: [UserInput]): [User]
                            }
                            input UserInput {
                                userId: ID
                                site: String!
                            }
                            type User {
                                id: ID!
                                name: String!
                                siteId: ID
                            }
                        """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.isNotEmpty())
            val error = errors.singleOfType<IncompatibleHydrationInputObjectArgumentType>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creators")
            assert(error.remoteArg.name == "id")
            assert(error.actorFieldName == "site")
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "Int!")
            assert(GraphQLTypeUtil.simplePrint(error.actorFieldType) == "String!")
        }

        context("passes for acceptable batch hydration cases") {
            withData(
                nameFn = { (hydration, actor) -> "Hydration=$hydration, actor=$actor" },
                "ID" to "ID",
                "[ID]" to "ID",
                "ID" to "[ID]",
                "ID!" to "[ID]",
                "ID!" to "[ID]!",
                "ID!" to "[ID!]",

                ) { (hydration, actor) ->
                val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "issues" to """
                            type Query {
                                issue: JiraIssue
                            }
                            type JiraIssue @renamed(from: "Issue") {
                                id: ID!
                                creators: [User] @hydrated(
                                    service: "users"
                                    field: "usersById"
                                    arguments: [
                                        {name: "id", value: "$source.creators.id"}
                                    ]
                                    identifiedBy: "id"
                                )
                            }
                        """.trimIndent(),
                        "users" to """
                            type Query {
                                usersById(id: $actor): [User]
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
                                creators: [CreatorRef]
                            }
                            type CreatorRef {
                                id: $hydration
                            }
                        """.trimIndent(),
                        "users" to """
                            type Query {
                                usersById(id: $actor): [User]
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
        }
    }
})
