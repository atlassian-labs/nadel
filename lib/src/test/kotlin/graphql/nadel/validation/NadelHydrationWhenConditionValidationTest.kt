package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.SomeHydrationsHaveMissingConditions
import graphql.nadel.validation.NadelSchemaValidationError.WhenConditionPredicateDoesNotMatchSourceFieldType
import graphql.nadel.validation.NadelSchemaValidationError.WhenConditionPredicateRequiresStringSourceField
import graphql.nadel.validation.NadelSchemaValidationError.WhenConditionSourceFieldDoesNotExist
import graphql.nadel.validation.NadelSchemaValidationError.WhenConditionUnsupportedFieldType
import graphql.nadel.validation.util.assertSingleOfType
import io.kotest.core.spec.style.DescribeSpec

private const val source = "$" + "source"
private const val argument = "$" + "argument"

class NadelHydrationWhenConditionValidationTest : DescribeSpec({
    describe("Hydration when condition validation") {
        it("happy path (valid when condition)") {
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
                            type: String
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
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
                            creator: ID!
                            type: String!
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
        it("passes if sourceField is simple values Int") {
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
                            rank: Int
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "rank"
                                        predicate: { equals: 1 }
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
                            creator: ID!
                            rank: Int!
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
        it("passes if expecting an Int for an ID sourceField") {
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
                            issueId: ID
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "issueId"
                                        predicate: { equals: 1 }
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
                            creator: ID!
                            issueId: ID!
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
        it("passes if expecting a String for an ID sourceField") {
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
                            issueId: ID
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "issueId"
                                        predicate: { equals: "ID123" }
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
                            creator: ID!
                            issueId: ID!
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
        it("fails if sourceField is not a value of String, Int, or ID") {
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
                            valid: Boolean
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "valid"
                                        predicate: { equals: true }
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
                            creator: ID!
                            valid: Boolean!
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

            val error = errors.assertSingleOfType<WhenConditionUnsupportedFieldType>()
            assert(error.overallField.name == "creator")
            assert(error.sourceFieldName == "valid")
            assert(error.sourceFieldTypeName == "Boolean")
        }
        it("fails if sourceField is array of valid value") {
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
                            categories: [String]
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "categories"
                                        predicate: { equals: ["category1"] }
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
                            creator: ID!
                            categories: [String]!
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

            val error = errors.assertSingleOfType<WhenConditionUnsupportedFieldType>()
            assert(error.overallField.name == "creator")
            assert(error.sourceFieldName == "categories")
            assert(error.sourceFieldTypeName == "[String]")
        }
        it("fails if sourceField doesnt exist") {
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
                                when: {
                                    result: {
                                        sourceField: "type"
                                        predicate: { equals: "type1" }
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

            val error = errors.assertSingleOfType<WhenConditionSourceFieldDoesNotExist>()
            assert(error.overallField.name == "creator")
            assert(error.sourceFieldName == "type")
        }
        it("equals predicate fails if expected type mismatches with field type") {
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
                            type: String
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "type"
                                        predicate: { equals: 123 }
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
                            creator: ID!
                            type: String!
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

            val error = errors.assertSingleOfType<WhenConditionPredicateDoesNotMatchSourceFieldType>()
            assert(error.overallField.name == "creator")
            assert(error.sourceFieldName == "type")
            assert(error.sourceFieldTypeName == "String")
            assert(error.predicateTypeName == "BigInteger")
        }

        it("startsWith predicate works on String field") {
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
                            type: String
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "type"
                                        predicate: { startsWith: "somePrefix" }
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
                            creator: ID!
                            type: String!
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
        it("startsWith predicate works on ID field") {
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
                            issueID: ID
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "issueID"
                                        predicate: { startsWith: "somePrefix" }
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
                            creator: ID!
                            issueID: ID!
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
        it("startsWith predicate fails on non-string field") {
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
                            size: Int
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "size"
                                        predicate: { startsWith: "1" }
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
                            creator: ID!
                            size: Int!
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

            val error = errors.assertSingleOfType<WhenConditionPredicateRequiresStringSourceField>()
            assert(error.overallField.name == "creator")
            assert(error.sourceFieldName == "size")
            assert(error.sourceFieldTypeName == "Int")
            assert(error.predicateType == "startsWith")
        }

        it("matches predicate works on String field") {
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
                            type: String
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "type"
                                        predicate: { matches: "someRegex" }
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
                            creator: ID!
                            type: String!
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
        it("matches predicate works on ID field") {
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
                            issueID: ID
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "issueID"
                                        predicate: { matches: "someRegex" }
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
                            creator: ID!
                            issueID: ID!
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
        it("matches predicate fails on non-string field") {
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
                            size: Int
                            creator: User @hydrated(
                                service: "users"
                                field: "user"
                                arguments: [
                                    {name: "id", value: "$source.creator"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "size"
                                        predicate: { matches: "someRegex" }
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
                            creator: ID!
                            size: Int!
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

            val error = errors.assertSingleOfType<WhenConditionPredicateRequiresStringSourceField>()
            assert(error.overallField.name == "creator")
            assert(error.sourceFieldName == "size")
            assert(error.sourceFieldTypeName == "Int")
            assert(error.predicateType == "matches")
        }

        it("passes with multiple hydrations all with when conditions") {
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
                        type OtherUser {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            type: String
                            creator: UserResult 
                                @hydrated(
                                    service: "users"
                                    field: "user"
                                    arguments: [
                                        {name: "id", value: "$source.creator"}
                                    ]
                                    when: {
                                        result: {
                                            sourceField: "type"
                                            predicate: { equals: "someTypeOfIssue" }
                                        }
                                    }
                                )
                                @hydrated(
                                    service: "users"
                                    field: "user"
                                    arguments: [
                                        {name: "id", value: "$source.creator"}
                                    ]
                                    when: {
                                        result: {
                                            sourceField: "type"
                                            predicate: { equals: "someOtherTypeOfIssue" }
                                        }
                                    }
                                )
                        }
                        union UserResult = User | OtherUser
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
                            type: String!
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
                        type OtherUser {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
            )
            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }
        it("passes with multiple hydrations all without when conditions") {
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
                        type OtherUser {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            type: String
                            creator: UserResult 
                                @hydrated(
                                    service: "users"
                                    field: "user"
                                    arguments: [
                                        {name: "id", value: "$source.creator"}
                                    ]
                                )
                                @hydrated(
                                    service: "users"
                                    field: "user"
                                    arguments: [
                                        {name: "id", value: "$source.creator"}
                                    ]
                                )
                        }
                        union UserResult = User | OtherUser
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
                            type: String!
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
                        type OtherUser {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
            )
            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }
        it("fails if some hydrations are missing a when condition") {
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
                        type OtherUser {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            type: String
                            creator: UserResult 
                                @hydrated(
                                    service: "users"
                                    field: "user"
                                    arguments: [
                                        {name: "id", value: "$source.creator"}
                                    ]
                                    when: {
                                        result: {
                                            sourceField: "type"
                                            predicate: { equals: "someTypeOfIssue" }
                                        }
                                    }
                                )
                                @hydrated(
                                    service: "users"
                                    field: "user"
                                    arguments: [
                                        {name: "id", value: "$source.creator"}
                                    ]
                                )
                        }
                        union UserResult = User | OtherUser
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
                            type: String!
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
                        type OtherUser {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
                ),
            )
            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<SomeHydrationsHaveMissingConditions>()
            assert(error.overallField.name == "creator")
        }
    }
})