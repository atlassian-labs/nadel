package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldInHydratedInputObject
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleHydrationArgumentType
import graphql.nadel.validation.NadelSchemaValidationError.MissingFieldInHydratedInputObject
import graphql.nadel.validation.util.assertSingleOfType
import graphql.schema.GraphQLTypeUtil
import io.kotest.core.spec.style.DescribeSpec

private const val source = "$" + "source"
private const val argument = "$" + "argument"

class NadelHydrationArgumentValidationTest : DescribeSpec({
    describe("Hydration arg validation") {
        it("fails if the required actor field argument type does not match the supplied source field type") {
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

            // Assert that Field JiraIssue.creator tried to hydrate with argument id
            // using value from field Issue.creator from service issues of type of ID!
            // whereas the actor field requires an argument of type Boolean

            val error = errors.assertSingleOfType<IncompatibleHydrationArgumentType>()
            //actor field is JiraIssue.creator, being hydrated with arg "id" of type Boolean
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "id")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "Boolean")
            // supplied hydration field is: Issue.creator of type ID!
            assert(error.parentType.underlying.name == "Issue")
            assert(error.remoteArg.remoteArgumentSource.pathToField?.joinToString(separator = ".") == "creator")
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "ID!")


        } //CHECKED

        it("allows a source field of type String to be assigned to actor field argument of type ID") {
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
        } //CHECKED

        it("allows a source field of type Int to be assigned to actor field argument of type ID") {
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
                            creator: Int!
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
        } //CHECKED

        it("fails when trying to assign a source field of type ID to an actor field argument of type String") {
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

            // Assert that Field JiraIssue.creator tried to hydrate with argument id
            // using value from field Issue.creator from service issues of type ID!
            // whereas the actor field requires an argument of type String!

            val error = errors.assertSingleOfType<IncompatibleHydrationArgumentType>()
            //actor field is JiraIssue.creator, being hydrated with arg "id" of type String
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "id")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "String!")
            // supplied hydration field is: Issue.creator of type ID!
            assert(error.parentType.underlying.name == "Issue")
            assert(error.remoteArg.remoteArgumentSource.pathToField?.joinToString(separator = ".") == "creator")
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "ID!")
        } //CHECKED

        it("allows a non-null source field to be applied to a nullable actor field argument") {
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
                            user(id: ID): User
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
                            creator: ID!
                        }
                    """.trimIndent(),
                            "users" to """
                        type Query {
                            user(id: ID): User
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
        } //CHECKED

        it("fails when trying to assign a nullable source field of type ID to a non-null actor field argument") {
            val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                            "issues" to """
                        type Query {
                            issue: JiraIssue
                        }
                        type JiraIssue @renamed(from: "Issue") {
                            id: String!
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

            // Assert that Field JiraIssue.creator tried to hydrate with argument id
            // using value from field Issue.creator from service issues with an argument of String
            // whereas the actor field requires an argument of type String!

            val error = errors.assertSingleOfType<IncompatibleHydrationArgumentType>()
            //actor field is JiraIssue.creator, being hydrated with arg "id" of type String
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "id")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "String!")
            // supplied hydration field is: Issue.creator of type ID!
            assert(error.parentType.underlying.name == "Issue")
            assert(error.remoteArg.remoteArgumentSource.pathToField?.joinToString(separator = ".") == "creator")
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "String")
        } //CHECKED

        it("actor field array arg assignability is allowed (batch hydration)") {
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
        } //CHECKED

        it("Array of ID should not be assignable Array of String when validating hydration arguments") {
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

            // Assert that Field JiraIssue.creator tried to hydrate with argument ids
            // using value from field Issue.creators from service issues of type [ID!]!
            // whereas the actor field requires the argument to be of type [String!]!

            val error = errors.assertSingleOfType<IncompatibleHydrationArgumentType>()
            //actor field is JiraIssue.creator needs arg "ids" of type [String!]
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "ids")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "[String!]!")
            // supplied hydration field is: Issue.creators of type ID!
            assert(error.parentType.underlying.name == "Issue")
            assert(error.remoteArg.remoteArgumentSource.pathToField?.joinToString(separator = ".") == "creators")
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "[ID!]!")
        } //CHECKED

        it("Inner non-null check for arrays - Array of [Type]! should not be assignable Array of [Type!]! when validating hydration arguments") {
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

            // Assert that Field JiraIssue.creator tried to hydrate with argument ids
            // using value from field Issue.creators from service issues of type [ID!]!
            // whereas the actor field requires the argument to be of type [ID]!

            val error = errors.assertSingleOfType<IncompatibleHydrationArgumentType>()
            //actor field is JiraIssue.creator needs arg "ids" of type [String!]
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "ids")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "[ID!]!")
            // supplied hydration field is: Issue.creators of type ID!
            assert(error.parentType.underlying.name == "Issue")
            assert(error.remoteArg.remoteArgumentSource.pathToField?.joinToString(separator = ".") == "creators")
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "[ID]!")
        } //CHECKED

        it("testing array - compatible list of lists passes validation") {
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
        }  //CHECKED

        it("testing array - incompatible list of lists fails validation") {
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

            // Assert that Field JiraIssue.creator tried to hydrate with argument ids
            // using value from field Issue.creators from service issues of type [ID!]!
            // whereas the actor field requires the argument to be of type [String!]!

            val error = errors.assertSingleOfType<IncompatibleHydrationArgumentType>()
            //actor field is JiraIssue.creator needs arg "ids" of type [String!]
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "ids")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "[[String!]!]!")
            // supplied hydration field is: Issue.creators of type ID!
            assert(error.parentType.underlying.name == "Issue")
            assert(error.remoteArg.remoteArgumentSource.pathToField?.joinToString(separator = ".") == "creators")
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "[[ID!]!]!")
        } //CHECKED

        it("input object - validation allows compatible input objects") {
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
                        input FullNameInput {
                            first: String!
                            last: String!
                        }
                        type Query {
                            user(name: FullNameInput!): User
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
                                    {name: "name", value: "$source.creator"}
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
                            creator: FullName!
                        }
                        type FullName {
                            first: String!
                            last: String!
                        }
                        
                    """.trimIndent(),
                            "users" to """
                        input FullNameInput {
                            first: String!
                            last: String!
                        }
                        type Query {
                            user(name: FullNameInput!): User
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
        } //CHECKED

        it("input object - fails if incompatible input objects (missing field)") {
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
                        input FullNameInput {
                            first: String!
                            middleName: String!
                            last: String!
                        }
                        type Query {
                            user(name: FullNameInput!): User
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
                                    {name: "name", value: "$source.creator"}
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
                            creator: FullName!
                        }
                        type FullName {
                            first: String!
                            last: String!
                        }
                        
                    """.trimIndent(),
                            "users" to """
                        input FullNameInput {
                            first: String!
                            middleName: String!
                            last: String!
                        }
                        type Query {
                            user(name: FullNameInput!): User
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

            // Assert that Field JiraIssue.creator tried to hydrate with argument name
            // using value from field Issue.creator from service issues
            // but Issue.creator was missing the middleName field

            val error = errors.assertSingleOfType<MissingFieldInHydratedInputObject>()
            // assert the missing type is the correct one
            assert(error.missingFieldName == "middleName")
            //actor field is JiraIssue.creator, being hydrated with arg "id" of type String
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "name")
            // supplied hydration field is: Issue.creator of type ID!
            assert(error.parentType.underlying.name == "Issue")
            assert(error.remoteArg.remoteArgumentSource.pathToField?.joinToString(separator = ".") == "creator")
        } //CHECKED

        it("input object - respects null rules for fields inside objects") {
            // argument requires last name, but we are supplying a type with an optional last name
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
                        input FullNameInput {
                            first: String!
                            last: String!
                        }
                        type Query {
                            user(name: FullNameInput!): User
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
                                    {name: "name", value: "$source.creator"}
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
                            creator: FullName!
                        }
                        type FullName {
                            first: String!
                            last: String
                        }
                        
                    """.trimIndent(),
                            "users" to """
                        input FullNameInput {
                            first: String!
                            last: String!
                        }
                        type Query {
                            user(name: FullNameInput!): User
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

            // Field JiraIssue.creator tried to hydrate with "name" using value from Issue.creator
            // but the types are not compatible

            val error = errors.assertSingleOfType<IncompatibleFieldInHydratedInputObject>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "name")
            // supplied hydration field is: Issue.creator of type ID!
            assert(error.parentType.underlying.name == "Issue")
            assert(error.remoteArg.remoteArgumentSource.pathToField?.joinToString(separator = ".") == "creator")
        }
      /*
        it("input object - validates a type nested inside list nested inside object"){}

        it("testing array - validates an array of object type") {}

        //OTHERS
        it("fails if hydration argument source type is mismatch with actor field input arguments") {} //DONE
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
            val error = errors.assertSingleOfType<IncompatibleHydrationArgumentType>()
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
                                userId: ID!
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
                                userId: ID!
                            }
                        """.trimIndent(),
                            "users" to """
                            type Query {
                                users(id: [UserInput]): [User]
                            }
                            input UserInput {
                                userId: ID!
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
            val error = errors.assertSingleOfType<MissingFieldInHydratedInputObject>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creators")
            assert(error.remoteArg.name == "id")
            assert(error.missingFieldName == "site")
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
            val error = errors.assertSingleOfType<IncompatibleFieldInHydratedInputObject>()
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creators")
            assert(error.remoteArg.name == "id")
            assert(error.actorFieldName == "site")
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "Int!")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "String!")
        } */
    }
})