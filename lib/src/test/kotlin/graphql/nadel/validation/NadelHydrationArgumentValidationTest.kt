package graphql.nadel.validation

import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldInHydratedInputObject
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleHydrationArgumentType
import graphql.nadel.validation.NadelSchemaValidationError.MissingFieldInHydratedInputObject
import graphql.nadel.validation.NadelSchemaValidationError.NoSourceArgsInBatchHydration
import graphql.nadel.validation.NadelSchemaValidationError.StaticArgIsNotAssignable
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
                            user(id: Int): User
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
                            user(id: Int): User
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

            // Field "JiraIssue.creator" tried to hydrate with argument "id"
            // using the value from field "Issue.creator" from service "issues" of type ID!
            // but it was not assignable to the expected type Boolean

            val error = errors.assertSingleOfType<IncompatibleHydrationArgumentType>()
            //actor field arg:
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "id")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "Int")
            // supplied hydration for arg:
            assert(error.parentType.underlying.name == "Issue")
            val remoteArgumentSource = error.remoteArg.value as NadelHydrationArgumentDefinition.ValueSource.ObjectField
            assert(remoteArgumentSource.pathToField.joinToString(separator = ".") == "creator")
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "ID!")
        }

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
        }

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
        }

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
        }

        it("allows a nullable source field to be applied to a non-null actor field argument") {
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

        it("fails when trying to assign a nullable input argument to a non-null actor field argument") {
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
                            creator(creatorId: ID): User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$argument.creatorId"}
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
            assert(errors.map { it.message }.isNotEmpty())
            // Field "JiraIssue.creator" tried to hydrate with argument "id"
            // using the supplied argument called "creatorId" of type ID,
            // but it was not assignable to the expected type ID!

            val error = errors.assertSingleOfType<IncompatibleHydrationArgumentType>()
            // required actor field arg:
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "id")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "ID!")
            // supplied hydration for arg:
            val remoteArgumentSource = error.remoteArg.value as NadelHydrationArgumentDefinition.ValueSource.FieldArgument
            assert(remoteArgumentSource.argumentName == "creatorId")
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "ID")
        }

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
        }

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
        }

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
                            creators: [[Int!]!]!
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
            // required actor field arg:
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "ids")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "[[String!]!]!")
            // supplied hydration for arg:
            assert(error.parentType.underlying.name == "Issue")
            val remoteArgumentSource = error.remoteArg.value as NadelHydrationArgumentDefinition.ValueSource.ObjectField
            assert(remoteArgumentSource.pathToField.joinToString(separator = ".") == "creators")
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "[[Int!]!]!")
        }

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
        }

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
            // required actor field arg:
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "name")
            // supplied hydration for arg:
            assert(error.parentType.underlying.name == "Issue")
            val remoteArgumentSource = error.remoteArg.value as NadelHydrationArgumentDefinition.ValueSource.ObjectField
            assert(remoteArgumentSource.pathToField.joinToString(separator = ".") == "creator")
        }

        it("input object - validation allows a valid array nested inside object") {
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
                            middleNames: [String]!
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
                            middleNames: [String]!
                            last: String!
                        }
                        
                    """.trimIndent(),
                    "users" to """
                        input FullNameInput {
                            first: String!
                            middleNames: [String]!
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
        }

        it("input object - allows a valid input nested inside an input") {
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
                       input UserBasicInfo {
                            name: FullNameInput!
                            dob: String!
                       }
                        input FullNameInput {
                            first: String!
                            last: String!
                        }
                        type Query {
                            user(userInfo: UserBasicInfo!): User
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
                                    {name: "userInfo", value: "$source.creator"}
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
                            creator: UserBasicInfo!
                        }
                       type UserBasicInfo {
                            name: FullName!
                            dob: String!
                       }
                        type FullName {
                            first: String!
                            last: String!
                        }
                        
                    """.trimIndent(),
                    "users" to """
                       input UserBasicInfo {
                            name: FullNameInput!
                            dob: String!
                       }
                        input FullNameInput {
                            first: String!
                            last: String!
                        }
                        type Query {
                            user(userInfo: UserBasicInfo!): User
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

        it("input object - fails validation an invalid type inside input inside another input") {
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
                       input UserBasicInfo {
                            name: FullNameInput!
                            dob: String!
                       }
                        input FullNameInput {
                            first: String!
                            last: Int!
                        }
                        type Query {
                            user(userInfo: UserBasicInfo!): User
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
                                    {name: "userInfo", value: "$source.creator"}
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
                            creator: UserBasicInfo!
                        }
                       type UserBasicInfo {
                            name: FullName!
                            dob: String!
                       }
                        type FullName {
                            first: String!
                            last: String!
                        }
                        
                    """.trimIndent(),
                    "users" to """
                       input UserBasicInfo {
                            name: FullNameInput!
                            dob: String!
                       }
                        input FullNameInput {
                            first: String!
                            last: Int!
                        }
                        type Query {
                            user(userInfo: UserBasicInfo!): User
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

            // Field JiraIssue.creator tried to hydrate with argument "userInfo"
            // using value from Issue.creator but the types are not compatible
            val error = errors.assertSingleOfType<IncompatibleFieldInHydratedInputObject>()
            // required actor field arg:
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "userInfo")
            // supplied hydration for arg:
            assert(error.parentType.underlying.name == "Issue")
            val remoteArgumentSource = error.remoteArg.value as NadelHydrationArgumentDefinition.ValueSource.ObjectField
            assert(remoteArgumentSource.pathToField.joinToString(separator = ".") == "creator")

        }

        it("testing array - allows a compatible array of objects") {
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
                            users(names: [FullNameInput]!): [User]
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
                                    {name: "names", value: "$source.creators"}
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
                            creators: [FullName]!
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
                            users(names: [FullNameInput]!): [User]
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

        it("testing array - validation fails on incompatible array of objects") {
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
                            middle: String!
                            last: String!
                        }
                        type Query {
                            users(names: [FullNameInput]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            creators: [User] @hydrated(
                                service: "users"
                                field: "users"
                                arguments: [
                                    {name: "names", value: "$source.creators"}
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
                            creators: [FullName]!
                        }
                        type FullName {
                            first: String!
                            last: String!
                        }
                        
                    """.trimIndent(),
                    "users" to """
                        input FullNameInput {
                            first: String!
                            middle: String!
                            last: String!
                        }
                        type Query {
                            users(names: [FullNameInput]!): [User]
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

            // Field JiraIssue.creator tried to hydrate with argument names
            // using value from field Issue.creators from service issues of type [FullName]!
            // whereas the actor field requires the argument to be of type [FullNameInput]!

            val error = errors.assertSingleOfType<IncompatibleHydrationArgumentType>()
            // required actor field arg:
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creators")
            assert(error.remoteArg.name == "names")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "[FullNameInput]!")
            // supplied hydration for arg:
            assert(error.parentType.underlying.name == "Issue")
            val remoteArgumentSource = error.remoteArg.value as NadelHydrationArgumentDefinition.ValueSource.ObjectField
            assert(remoteArgumentSource.pathToField.joinToString(separator = ".") == "creators")
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "[FullName]!")

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

            // Field JiraIssue.creator tried to hydrate with argument someArg
            // using value from field Issue.null from service issues of type ID!
            // whereas the actor field requires the argument to be of type Int!

            val error = errors.assertSingleOfType<IncompatibleHydrationArgumentType>()
            // required actor field arg:
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "someArg")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "Int!")
            // supplied hydration for arg:
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "ID!")

        }

        it("does arg validation for object types when doing batch hydration") {
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
            // Field JiraIssue.creators tried to hydrate with argument id
            // using value from field Issue.creators from service issues of type [UserRef]
            // whereas the actor field requires the argument to be of type [UserInput]

            val error = errors.assertSingleOfType<IncompatibleHydrationArgumentType>()
            // required actor field arg:
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creators")
            assert(error.remoteArg.name == "id")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "[UserInput]")
            // supplied hydration for arg:
            assert(error.parentType.underlying.name == "Issue")
            val remoteArgumentSource = error.remoteArg.value as NadelHydrationArgumentDefinition.ValueSource.ObjectField
            assert(remoteArgumentSource.pathToField.joinToString(separator = ".") == "creators")
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "[UserRef]")
        }

        it("Batch hydration edge case - feeding an ID into an [ID] arg is allowed") {
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

        it("non-batch ManyToOne edge case - feeding an [ID] into an ID arg is allowed") {
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

        it("passes a valid enum hydration argument") {
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
                            user(
                                id: ID!
                                providerType: ProviderType
                            ): User
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        enum ProviderType {
                            DEV_INFO
                            BUILD
                            DEPLOYMENT
                            FEATURE_FLAG
                            REMOTE_LINKS
                            SECURITY
                            DOCUMENTATION
                            DESIGN
                            OPERATIONS
                        }
                        extend type JiraIssue {
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"},
                                    { name: "providerType", value: "$source.providerType" },
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
                            providerType: ProviderType   
                        }
                        enum ProviderType {
                            DEV_INFO
                            BUILD
                            DEPLOYMENT
                            FEATURE_FLAG
                            REMOTE_LINKS
                            SECURITY
                            DOCUMENTATION
                            DESIGN
                            OPERATIONS
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            user(
                                id: ID!, 
                                providerType: ProviderType
                            ): User
                        }
                        type User {
                            id: ID!
                            name: String!
                            providerType: ProviderType
                        }
                        enum ProviderType {
                            DEV_INFO,
                            BUILD,
                            DEPLOYMENT,
                            FEATURE_FLAG,
                            REMOTE_LINKS,
                            SECURITY,
                            DOCUMENTATION,
                            DESIGN,
                            OPERATIONS
                        }
                    """.trimIndent(),
                ),
            )
            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        it("fails validation on mismatching enums hydration argument") {
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
                            user(
                                id: ID!
                                providerType: SomeOtherEnumType
                            ): User
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        enum SomeOtherEnumType {
                            DEV_INFO
                            BUILD
                        }
                        extend type JiraIssue {
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"},
                                    { name: "providerType", value: "$source.providerType" },
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
                            providerType: ProviderType   
                        }
                        enum ProviderType {
                            DEV_INFO
                            BUILD
                            DEPLOYMENT
                            FEATURE_FLAG
                            REMOTE_LINKS
                            SECURITY
                            DOCUMENTATION
                            DESIGN
                            OPERATIONS
                        }
                    """.trimIndent(),
                    "users" to """
                        type Query {
                            user(
                                id: ID!
                                name: String  
                                providerType: SomeOtherEnumType
                            ): User
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        enum SomeOtherEnumType {
                            DEV_INFO,
                            BUILD,
                        }
                    """.trimIndent(),
                ),
            )
            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            // Field "JiraIssue.creator" tried to hydrate using the actor field "user" and argument "providerType".
            // However, you are supplying actor field argument with the value from field "Issue.providerType" from service "issues" of type ProviderType
            // which is not assignable to the expected type SomeOtherEnumType

            val error = errors.assertSingleOfType<IncompatibleHydrationArgumentType>()
            //actor field arg:
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "providerType")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "SomeOtherEnumType")
            // supplied hydration for arg:
            assert(error.parentType.underlying.name == "Issue")
            val remoteArgumentSource = error.remoteArg.value as NadelHydrationArgumentDefinition.ValueSource.ObjectField
            assert(remoteArgumentSource.pathToField.joinToString(separator = ".") == "providerType")
            assert(GraphQLTypeUtil.simplePrint(error.hydrationType) == "ProviderType")
        }
    }
    describe("Hydration static arg validation") {
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
                            user(id: Int): User
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
                                    {name: "id", value: "someStaticString"}
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
                            user(id: Int): User
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

            // Field JiraIssue.creator tried to hydrate with argument id of type Int
            // using a statically supplied argument,
            // but the type of the supplied static argument is incompatible

            val error = errors.assertSingleOfType<StaticArgIsNotAssignable>()
            //actor field arg:
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "id")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "Int")
        }

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
                                    {name: "id", value: "someStaticString"}
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
                                    {name: "id", value: 123}
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

        it("batch hydration is not allowed with only static args (i.e. when there is no \$source arg)") {
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
                                    {name: "ids", value: ["id1", "id2", "id3"]}
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
            errors.assertSingleOfType<NoSourceArgsInBatchHydration>()
        }

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
                            user(ids: [[ID!]!]!): User
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
                                    {name: "ids", value: [["id1","id2","id3"],["id4","id5","id6"]]}
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
                            user(ids: [[ID!]!]!): User
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
                            user(ids: [[String!]!]!): User
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
                                    {name: "ids", value: [[1,2],[3,4],[5,6]]}
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
                            user(ids: [[String!]!]!): User
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

            // Field JiraIssue.creator tried to hydrate with argument ids of type [[String!]!]!
            // using a statically supplied argument
            // but the type of the supplied static argument is incompatible

            val error = errors.assertSingleOfType<StaticArgIsNotAssignable>()
            //actor field arg:
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "ids")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "[[String!]!]!")
        }

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
                                    {name: "name", value: {first: "big", last: "boi"}}
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
        }

        it("fails if incompatible input objects (missing field)") {
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
                                    {name: "name", value: {first: "Fried", last: "Chicken"}}
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

            // Field JiraIssue.creator tried to hydrate with argument name of type FullNameInput!
            // using a statically supplied argument
            // but the type of the supplied static argument is incompatible

            val error = errors.assertSingleOfType<StaticArgIsNotAssignable>()
            //actor field arg:
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "name")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "FullNameInput!")
        }

        it("validation allows a valid array nested inside object") {
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
                            middleNames: [String]!
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
                                    {
                                        name: "name", 
                                        value: {
                                            first: "Frank",
                                            middleNames: ["Chicken", "Store", "Locator"],
                                            last: "Lin"
                                        }
                                    }
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
                            middleNames: [String]!
                            last: String!
                        }
                        
                    """.trimIndent(),
                    "users" to """
                        input FullNameInput {
                            first: String!
                            middleNames: [String]!
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
        }

        it("allows a valid object nested inside an object") {
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
                       input UserBasicInfo {
                            name: FullNameInput!
                            dob: String!
                       }
                        input FullNameInput {
                            first: String!
                            last: String!
                        }
                        type Query {
                            user(userInfo: UserBasicInfo!): User
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
                                    {
                                            name: "userInfo", 
                                            value: {
                                                name: {
                                                    first: "covid"
                                                    last: "baby"
                                                }
                                                dob: "04-04-2020"
                                            }
                                    }
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
                            creator: UserBasicInfo!
                        }
                       type UserBasicInfo {
                            name: FullName!
                            dob: String!
                       }
                        type FullName {
                            first: String!
                            last: String!
                        }
                        
                    """.trimIndent(),
                    "users" to """
                       input UserBasicInfo {
                            name: FullNameInput!
                            dob: String!
                       }
                        input FullNameInput {
                            first: String!
                            last: String!
                        }
                        type Query {
                            user(userInfo: UserBasicInfo!): User
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

        it("fails validation an invalid scalar type inside object inside another object") {
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
                       input UserBasicInfo {
                            name: FullNameInput!
                            dob: String!
                       }
                        input FullNameInput {
                            first: String!
                            last: Int!
                        }
                        type Query {
                            user(userInfo: UserBasicInfo!): User
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
                                    {
                                        name: "userInfo", 
                                        value: {
                                            name: {
                                                first: "covid"
                                                last: "baby"
                                            }
                                            dob: "04-04-2020"
                                        }
                                    }
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
                            creator: UserBasicInfo!
                        }
                       type UserBasicInfo {
                            name: FullName!
                            dob: String!
                       }
                        type FullName {
                            first: String!
                            last: String!
                        }
                        
                    """.trimIndent(),
                    "users" to """
                       input UserBasicInfo {
                            name: FullNameInput!
                            dob: String!
                       }
                        input FullNameInput {
                            first: String!
                            last: Int!
                        }
                        type Query {
                            user(userInfo: UserBasicInfo!): User
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

            // Field JiraIssue.creator tried to hydrate with argument userInfo of type UserBasicInfo!
            // using a statically supplied argument,
            // but the type of the supplied static argument is incompatible

            val error = errors.assertSingleOfType<StaticArgIsNotAssignable>()
            //actor field arg:
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "userInfo")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "UserBasicInfo!")

        }

        it("testing array - allows a compatible array of objects") {
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
                            oldestUser(names: [FullNameInput]!): User
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            creator: User @hydrated(
                                service: "users"
                                field: "oldestUser"
                                arguments: [
                                    {
                                        name: "names", 
                                        value: [
                                            {
                                                first: "Frank",
                                                last: "Lin"
                                            },
                                            {
                                                first: "Brad",
                                                last: "Lee"
                                            },
                                            {
                                                first: "Anne",
                                                last: "Dee"
                                            }
                                        ]
                                    }
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
                            creators: [FullName]!
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
                            oldestUser(names: [FullNameInput]!): User
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

        it("testing array - validation fails on incompatible array of objects") {
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
                            middle: String!
                            last: String!
                        }
                        type Query {
                            oldestUser(names: [FullNameInput]!): User
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            creator: User @hydrated(
                                service: "users"
                                field: "oldestUser"
                                arguments: [
                                    {
                                        name: "names", 
                                        value: [
                                            {
                                                first: "Frank",
                                                last: "Lin"
                                            },
                                            {
                                                first: "Brad",
                                                last: "Lee"
                                            },
                                            {
                                                first: "Anne",
                                                last: "Dee"
                                            }
                                        ]
                                    }
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
                            creators: [FullName]!
                        }
                        type FullName {
                            first: String!
                            last: String!
                        }
                        
                    """.trimIndent(),
                    "users" to """
                        input FullNameInput {
                            first: String!
                            middle: String!
                            last: String!
                        }
                        type Query {
                            oldestUser(names: [FullNameInput]!): User
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

            // Field JiraIssue.creators tried to hydrate with argument names of type [FullNameInput]!
            // using a statically supplied argument,
            // but the type of the supplied static argument is incompatible

            val error = errors.assertSingleOfType<StaticArgIsNotAssignable>()
            //actor field arg:
            assert(error.parentType.overall.name == "JiraIssue")
            assert(error.overallField.name == "creator")
            assert(error.remoteArg.name == "names")
            assert(GraphQLTypeUtil.simplePrint(error.actorArgInputType) == "[FullNameInput]!")
        }
    }
})
