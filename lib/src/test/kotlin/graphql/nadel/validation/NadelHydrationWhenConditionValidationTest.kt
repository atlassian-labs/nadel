package graphql.nadel.validation

import graphql.nadel.validation.util.assertSingleOfType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val source = "$" + "source"
private const val argument = "$" + "argument"

class NadelHydrationWhenConditionValidationTest {
    private fun booleanConditionFixture(
        conditionFieldType: String = "Boolean",
        underlyingConditionFieldType: String = "Boolean!",
        predicate: String = "equals: true",
    ): NadelValidationTestFixture {
        return NadelValidationTestFixture(
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
                            users(id: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            shouldHydrate: $conditionFieldType
                            collaborators: [User] @hydrated(
                                service: "users"
                                field: "users"
                                arguments: [
                                    {name: "id", value: "$source.collaboratorIds"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "shouldHydrate"
                                        predicate: { $predicate }
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
                            collaboratorIds: [ID!]
                            shouldHydrate: $underlyingConditionFieldType
                        }
                    """.trimIndent(),
                "users" to """
                        type Query {
                            users(id: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
            ),
        )
    }

    private fun enumConditionFixture(
        conditionFieldType: String = "IssueType",
        underlyingConditionFieldType: String = "IssueType!",
        predicate: String = "equals: \"BUG\"",
    ): NadelValidationTestFixture {
        return NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to """
                        type Query {
                            issue: JiraIssue
                        }
                        type JiraIssue @renamed(from: "Issue") {
                            id: ID!
                        }
                        enum IssueType {
                            BUG
                            STORY
                        }
                    """.trimIndent(),
                "users" to """
                        type Query {
                            users(id: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            type: $conditionFieldType
                            collaborators: [User] @hydrated(
                                service: "users"
                                field: "users"
                                arguments: [
                                    {name: "id", value: "$source.collaboratorIds"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "type"
                                        predicate: { $predicate }
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
                        enum IssueType {
                            BUG
                            STORY
                        }
                        type Issue {
                            id: ID!
                            collaboratorIds: [ID!]
                            type: $underlyingConditionFieldType
                        }
                    """.trimIndent(),
                "users" to """
                        type Query {
                            users(id: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
            ),
        )
    }

    @Test
    fun `boolean condition field is acceptable for true equals predicate`() {
        val fixture = booleanConditionFixture()

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `boolean condition field is acceptable for false equals predicate`() {
        val fixture = booleanConditionFixture(predicate = "equals: false")

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `non null boolean condition field is acceptable for equals predicate`() {
        val fixture = booleanConditionFixture(conditionFieldType = "Boolean!")

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `list boolean condition field is not acceptable for equals predicate`() {
        val fixture = booleanConditionFixture(
            conditionFieldType = "[Boolean]",
            underlyingConditionFieldType = "[Boolean]",
        )

        // When
        val errors = validate(fixture)

        // Then
        errors.assertSingleOfType<NadelHydrationResultConditionUnsupportedFieldTypeError>()
    }

    @Test
    fun `boolean condition field rejects string equals value`() {
        val fixture = booleanConditionFixture(predicate = "equals: \"true\"")

        // When
        val errors = validate(fixture)

        // Then
        errors.assertSingleOfType<NadelHydrationConditionIncompatibleValueError>()
    }

    @Test
    fun `boolean condition field rejects matches predicate`() {
        val fixture = booleanConditionFixture(predicate = "matches: \"true\"")

        // When
        val errors = validate(fixture)

        // Then
        errors.assertSingleOfType<NadelHydrationConditionMatchesPredicateRequiresStringFieldError>()
    }

    @Test
    fun `boolean condition field rejects startsWith predicate`() {
        val fixture = booleanConditionFixture(predicate = "startsWith: \"tr\"")

        // When
        val errors = validate(fixture)

        // Then
        errors.assertSingleOfType<NadelHydrationConditionStartsWithPredicateRequiresStringFieldError>()
    }

    @Test
    fun `list type field is acceptable if its the source field`() {
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
                            users(id: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            type: String
                            collaborators: [User] @hydrated(
                                service: "users"
                                field: "users"
                                arguments: [
                                    {name: "id", value: "$source.collaboratorIds"}
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
                            collaboratorIds: [ID!]
                            type: String!
                        }
                    """.trimIndent(),
                "users" to """
                        type Query {
                            users(id: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `enum condition field is acceptable for equals predicate`() {
        val fixture = enumConditionFixture()

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `non null enum condition field is acceptable for equals predicate`() {
        val fixture = enumConditionFixture(conditionFieldType = "IssueType!")

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `list enum condition field is not acceptable for equals predicate`() {
        val fixture = enumConditionFixture(
            conditionFieldType = "[IssueType]",
            underlyingConditionFieldType = "[IssueType]",
        )

        // When
        val errors = validate(fixture)

        // Then
        errors.assertSingleOfType<NadelHydrationResultConditionUnsupportedFieldTypeError>()
    }

    @Test
    fun `enum condition field rejects invalid equals value`() {
        val fixture = enumConditionFixture(predicate = "equals: \"TASK\"")

        // When
        val errors = validate(fixture)

        // Then
        val error = errors.assertSingleOfType<NadelHydrationConditionInvalidEnumValueError>()
        assertEquals("TASK", error.suppliedValue)
    }

    @Test
    fun `enum condition field equals comparison is case sensitive`() {
        val fixture = enumConditionFixture(predicate = "equals: \"bug\"")

        // When
        val errors = validate(fixture)

        // Then
        val error = errors.assertSingleOfType<NadelHydrationConditionInvalidEnumValueError>()
        assertEquals("bug", error.suppliedValue)
    }

    @Test
    fun `enum condition field rejects empty string equals value`() {
        val fixture = enumConditionFixture(predicate = "equals: \"\"")

        // When
        val errors = validate(fixture)

        // Then
        val error = errors.assertSingleOfType<NadelHydrationConditionInvalidEnumValueError>()
        assertEquals("", error.suppliedValue)
    }

    @Test
    fun `enum condition field rejects matches predicate`() {
        val fixture = enumConditionFixture(predicate = "matches: \"BUG\"")

        // When
        val errors = validate(fixture)

        // Then
        errors.assertSingleOfType<NadelHydrationConditionMatchesPredicateRequiresStringFieldError>()
    }

    @Test
    fun `enum condition field rejects startsWith predicate`() {
        val fixture = enumConditionFixture(predicate = "startsWith: \"BU\"")

        // When
        val errors = validate(fixture)

        // Then
        errors.assertSingleOfType<NadelHydrationConditionStartsWithPredicateRequiresStringFieldError>()
    }

    @Test
    fun `list enum condition field is acceptable if its the batch source field`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to """
                        type Query {
                            issue: JiraIssue
                        }
                        enum IssueType {
                            BUG
                            STORY
                        }
                        type JiraIssue @renamed(from: "Issue") {
                            id: ID!
                            types: [IssueType]
                        }
                    """.trimIndent(),
                "users" to """
                        type Query {
                            users(id: [UserIssueType]): [User]
                        }
                        enum UserIssueType {
                            BUG
                            STORY
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            collaborators: [User] @hydrated(
                                service: "users"
                                field: "users"
                                arguments: [
                                    {name: "id", value: "$source.types"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "types"
                                        predicate: { equals: "BUG" }
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
                        enum IssueType {
                            BUG
                            STORY
                        }
                        type Issue {
                            id: ID!
                            types: [IssueType]
                        }
                    """.trimIndent(),
                "users" to """
                        type Query {
                            users(id: [UserIssueType]): [User]
                        }
                        enum UserIssueType {
                            BUG
                            STORY
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `renamed enum condition field is acceptable for equals predicate`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "issues" to """
                        type Query {
                            issue: JiraIssue
                        }
                        enum IssueType @renamed(from: "UnderlyingIssueType") {
                            BUG
                            STORY
                        }
                        type JiraIssue @renamed(from: "Issue") {
                            id: ID!
                            type: IssueType
                        }
                    """.trimIndent(),
                "users" to """
                        type Query {
                            users(id: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            collaborators: [User] @hydrated(
                                service: "users"
                                field: "users"
                                arguments: [
                                    {name: "id", value: "$source.collaboratorIds"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "type"
                                        predicate: { equals: "BUG" }
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
                        enum UnderlyingIssueType {
                            BUG
                            STORY
                        }
                        type Issue {
                            id: ID!
                            collaboratorIds: [ID!]
                            type: UnderlyingIssueType
                        }
                    """.trimIndent(),
                "users" to """
                        type Query {
                            users(id: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.map { it.message }.isEmpty())
    }

    @Test
    fun `matches predicate fails validation if it is invalid regex`() {
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
                            users(id: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                        extend type JiraIssue {
                            type: String
                            collaborators: [User] @hydrated(
                                service: "users"
                                field: "users"
                                arguments: [
                                    {name: "id", value: "$source.collaboratorIds"}
                                ]
                                when: {
                                    result: {
                                        sourceField: "type"
                                        predicate: { matches: "[a-z" }
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
                            collaboratorIds: [ID!]
                            type: String!
                        }
                    """.trimIndent(),
                "users" to """
                        type Query {
                            users(id: [ID!]!): [User]
                        }
                        type User {
                            id: ID!
                            name: String!
                        }
                    """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.map { it.message }.isNotEmpty())

        val error = errors.assertSingleOfType<NadelHydrationConditionInvalidRegexError>()
        assertTrue(error.virtualField.name == "collaborators")
        assertTrue(error.regexString == "[a-z")
    }
}
