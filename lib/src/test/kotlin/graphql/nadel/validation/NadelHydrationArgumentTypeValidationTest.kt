package graphql.nadel.validation

import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.isNullable
import graphql.nadel.engine.util.singleOfType
import graphql.nadel.util.makeUnderlyingSchema
import graphql.schema.GraphQLTypeUtil
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.Test
import kotlin.test.assertTrue

private const val source = "\$source"
private const val argument = "\$argument"

class NadelHydrationArgumentTypeValidationTest {
    @TestFactory
    fun `non-batched hydration failure if scalar source field cannot be assigned to object`(): List<DynamicTest> {
        data class TestFixture(
            val sourceType: String,
            val requiredType: String,
        )

        return listOf(
            "ID",
            "Boolean",
            "String",
            "Int",
        ).map { sourceType ->
            TestFixture(
                sourceType = sourceType,
                requiredType = "Search",
            )
        }.flatMap { fixture ->
            // Slap some wrappings on it
            listOf(
                fixture,
                fixture.copy(
                    sourceType = "${fixture.sourceType}!",
                    requiredType = "${fixture.requiredType}!",
                ),
                fixture.copy(
                    sourceType = "[${fixture.sourceType}]",
                    requiredType = "[${fixture.requiredType}]",
                ),
                fixture.copy(
                    sourceType = "[${fixture.sourceType}]!",
                    requiredType = "[${fixture.requiredType}]!",
                ),
                fixture.copy(
                    sourceType = "[${fixture.sourceType}!]",
                    requiredType = "[${fixture.requiredType}!]",
                ),
            )
        }.map { (sourceType, requiredType) ->
            DynamicTest.dynamicTest("$sourceType cannot be assigned to $requiredType") {
                val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "jira" to """
                            type Query {
                                issueById(search: $requiredType): JiraIssue
                            }
                            input Search {
                                author: String
                            }
                            type JiraIssue {
                                info: $sourceType
                                related: JiraIssue @hydrated(
                                    field: "issueById"
                                    arguments: [{ name: "search", value: "$source.info" }]
                                )
                            }
                        """.trimIndent(),
                    ),
                )

                // When
                val errors = validate(fixture)

                // Then
                assertTrue(errors.size == 1)

                val error = errors.singleOfType<NadelHydrationArgumentIncompatibleTypeError>()
                assertTrue(error.parentType.overall.name == "JiraIssue")
                assertTrue(error.virtualField.name == "related")
                assertTrue(error.hydration.backingField == listOf("issueById"))
                assertTrue(error.hydrationArgument.name == "search")
                assertTrue(GraphQLTypeUtil.simplePrint(error.suppliedType) == sourceType.filter(Char::isLetter))
                assertTrue(GraphQLTypeUtil.simplePrint(error.requiredType) == requiredType.filter(Char::isLetter))
            }
        }
    }

    @TestFactory
    fun `non-batched hydration success if scalar source field uses exact same scalar as backing argument`(): List<DynamicTest> {
        return listOf(
            "ID",
            "ID!",
            "[ID]",
            "[Boolean]",
            "Boolean",
            "String",
            "Int",
            "Int!",
        ).map { type ->
            DynamicTest.dynamicTest(type) {
                val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "jira" to """
                            type Query {
                                issueById(search: $type): JiraIssue
                            }
                            type JiraIssue {
                                info: $type
                                related: JiraIssue @hydrated(
                                    field: "issueById"
                                    arguments: [{ name: "search", value: "$source.info" }]
                                )
                            }
                        """.trimIndent(),
                    ),
                )

                // When
                val errors = validate(fixture)

                // Then
                assertTrue(errors.isEmpty())
            }
        }
    }

    @TestFactory
    fun `non-batched hydration success if source field scalar is compatible`(): List<DynamicTest> {
        data class TestFixture(val sourceType: String, val requiredType: String)
        return listOf(
            TestFixture(sourceType = "ID!", requiredType = "ID"),
            TestFixture(sourceType = "ID", requiredType = "ID"),

            // ID is string like enough that we need to accept this
            TestFixture(sourceType = "ID!", requiredType = "String"),
            TestFixture(sourceType = "ID", requiredType = "String"),

            // ID is number or text when used as input type
            TestFixture(sourceType = "String", requiredType = "ID"),
            TestFixture(sourceType = "Int", requiredType = "ID"),
            TestFixture(sourceType = "String!", requiredType = "ID"),
            TestFixture(sourceType = "Int!", requiredType = "ID"),
            TestFixture(sourceType = "[Int]!", requiredType = "[ID]"),
        ).map { (sourceType, requiredType) ->
            DynamicTest.dynamicTest("$sourceType can be assigned to $requiredType") {
                val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "jira" to """
                            type Query {
                                issueById(search: $requiredType): JiraIssue
                            }
                            type JiraIssue {
                                info: $sourceType
                                related: JiraIssue @hydrated(
                                    field: "issueById"
                                    arguments: [{ name: "search", value: "$source.info" }]
                                )
                            }
                        """.trimIndent(),
                    ),
                )

                // When
                val errors = validate(fixture)

                // Then
                assertTrue(errors.isEmpty())
            }
        }
    }

    @Test
    fun `non-batched hydration success if field output type can be assigned to input type`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to /*language=GraphQL*/ """
                    type Query {
                        issueById(search: SearchInput): JiraIssue
                    }
                    input SearchInput {
                        author: String!
                        location: LocationSearchInput
                    }
                    input LocationSearchInput {
                        city: String
                    }
                    type JiraIssue {
                        info: SearchInfo
                        related: JiraIssue @hydrated(
                            field: "issueById"
                            arguments: [{ name: "search", value: "$source.info" }]
                        )
                    }
                    type SearchInfo {
                        author: String!
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `non-batched hydration failure if object type is missing required field`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to /*language=GraphQL*/ """
                    type Query {
                        issueById(search: SearchInput): JiraIssue
                    }
                    input SearchInput {
                        author: String!
                        location: LocationSearchInput!
                    }
                    input LocationSearchInput {
                        city: String
                    }
                    type JiraIssue {
                        info: SearchInfo
                        related: JiraIssue @hydrated(
                            field: "issueById"
                            arguments: [{ name: "search", value: "$source.info" }]
                        )
                    }
                    type SearchInfo {
                        author: String!
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.size == 1)

        val error = errors.singleOfType<NadelHydrationArgumentMissingRequiredInputObjectFieldError>()
        assertTrue(error.parentType.overall.name == "JiraIssue")
        assertTrue(error.virtualField.name == "related")
        assertTrue(error.hydrationArgument.name == "search")
        assertTrue(error.suppliedFieldContainer.name == "SearchInfo")
        assertTrue(error.requiredFieldContainer.name == "SearchInput")
        assertTrue(error.requiredField.name == "location")
    }

    @Test
    fun `non-batched hydration failure if object type is missing required nested field`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to /*language=GraphQL*/ """
                    type Query {
                        issueById(search: SearchInput): JiraIssue
                    }
                    input SearchInput {
                        author: String
                        location: LocationSearchInput!
                    }
                    input LocationSearchInput {
                        city: String!
                    }
                    type JiraIssue {
                        info: SearchInfo
                        related: JiraIssue @hydrated(
                            field: "issueById"
                            arguments: [{ name: "search", value: "$source.info" }]
                        )
                    }
                    type SearchInfo {
                        author: String!
                        location: LocationSearchInfo!
                    }
                    type LocationSearchInfo {
                        asdf: String
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.size == 1)

        val error = errors.singleOfType<NadelHydrationArgumentMissingRequiredInputObjectFieldError>()
        assertTrue(error.parentType.overall.name == "JiraIssue")
        assertTrue(error.virtualField.name == "related")
        assertTrue(error.hydrationArgument.name == "search")
        assertTrue(error.suppliedFieldContainer.name == "LocationSearchInfo")
        assertTrue(error.requiredFieldContainer.name == "LocationSearchInput")
        assertTrue(error.requiredField.name == "city")
    }

    @Test
    fun `non-batched hydration failure if object type field exists but is nullable when input object requires non-nullable`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to /*language=GraphQL*/ """
                    type Query {
                        issueById(search: SearchInput): JiraIssue
                    }
                    input SearchInput {
                        author: String!
                        location: LocationSearchInput!
                    }
                    input LocationSearchInput {
                        city: String!
                    }
                    type JiraIssue {
                        info: SearchInfo
                        related: JiraIssue @hydrated(
                            field: "issueById"
                            arguments: [{ name: "search", value: "$source.info" }]
                        )
                    }
                    type SearchInfo {
                        author: String!
                        location: LocationSearchInfo!
                    }
                    type LocationSearchInfo {
                        city: String
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.size == 1)

        val error = errors.singleOfType<NadelHydrationIncompatibleInputObjectFieldError>()
        assertTrue(error.parentType.overall.name == "JiraIssue")
        assertTrue(error.virtualField.name == "related")
        val hydrationArgument = error.hydrationArgument
        assertTrue(hydrationArgument is NadelHydrationArgumentDefinition.ObjectField)
        assertTrue(hydrationArgument.name == "search")
        assertTrue(hydrationArgument.pathToField == listOf("info"))
        assertTrue(error.suppliedFieldContainer.name == "LocationSearchInfo")
        assertTrue(error.requiredFieldContainer.name == "LocationSearchInput")
        assertTrue(error.requiredField.name == "city")
        assertTrue(error.requiredField.type.isNonNull)
        assertTrue(error.suppliedField.name == "city")
        assertTrue(error.suppliedField.type.isNullable)
    }

    @Test
    fun `non-batched hydration succeeds if enum is the same`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to /*language=GraphQL*/ """
                    type Query {
                        issueById(search: IssueType): JiraIssue
                    }
                    enum IssueType {
                      Story
                      Task
                      Bug
                    }
                    type JiraIssue {
                        info: IssueType
                        related: JiraIssue @hydrated(
                            field: "issueById"
                            arguments: [{ name: "search", value: "$source.info" }]
                        )
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `non-batched hydration fails if required argument is missing`() {
        val fixture = NadelValidationTestFixture(
            overallSchema = mapOf(
                "jira" to /*language=GraphQL*/ """
                    type Query {
                        issueById(id: ID!, search: IssueType): JiraIssue
                    }
                    enum IssueType {
                      Story
                      Task
                      Bug
                    }
                    type JiraIssue {
                        info: IssueType
                        related: JiraIssue @hydrated(
                            field: "issueById"
                            arguments: [{ name: "search", value: "$source.info" }]
                        )
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.size == 1)

        val error = errors.singleOfType<NadelHydrationMissingRequiredBackingFieldArgumentError>()
        assertTrue(error.parentType.overall.name == "JiraIssue")
        assertTrue(error.virtualField.name == "related")
        assertTrue(error.hydration.backingField == listOf("issueById"))
        assertTrue(error.missingBackingArgument.name == "id")
    }

    @TestFactory
    fun `non-batched hydration succeeds if supplied argument matches backing argument type`(): List<DynamicTest> {
        data class TestFixture(
            val sourceType: String,
            val requiredType: String,
        )

        return listOf(
            TestFixture(sourceType = "String", requiredType = "String"),
            TestFixture(sourceType = "String!", requiredType = "String"),
            TestFixture(sourceType = "ID", requiredType = "String"),
            TestFixture(sourceType = "String", requiredType = "ID"),
            TestFixture(sourceType = "IssueType!", requiredType = "IssueType"),
            TestFixture(sourceType = "IssueType", requiredType = "IssueType"),
            TestFixture(sourceType = "Search", requiredType = "Search"),
            TestFixture(sourceType = "Search!", requiredType = "Search"),
        ).map { (sourceType, requiredType) ->
            DynamicTest.dynamicTest("$sourceType cannot be assigned to $requiredType") {
                val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "jira" to """
                            type Query {
                                issueById(search: $requiredType): JiraIssue
                            }
                            input Search {
                                author: String
                            }
                            enum IssueType {
                                Story
                            }
                            type JiraIssue {
                                id: ID
                                related(myArg: $sourceType): JiraIssue @hydrated(
                                    field: "issueById"
                                    arguments: [{ name: "search", value: "$argument.myArg" }]
                                )
                            }
                        """.trimIndent(),
                    ),
                )

                // When
                val errors = validate(fixture)

                // Then
                assertTrue(errors.isEmpty())
            }
        }
    }

    @TestFactory
    fun `non-batched hydration fails if supplied argument matches backing argument type`(): List<DynamicTest> {
        data class TestFixture(
            val sourceType: String,
            val requiredType: String,
        )

        return listOf(
            // Cannot assign nullable to not-nullable
            TestFixture(sourceType = "SearchQuery", requiredType = "SearchQuery!"),
            TestFixture(sourceType = "IssueType", requiredType = "IssueType!"),

            // Completely different types
            TestFixture(sourceType = "IssueType", requiredType = "SearchQuery!"),
            TestFixture(sourceType = "IssueType", requiredType = "SearchQuery"),
            TestFixture(sourceType = "SearchQuery", requiredType = "IssueType"),
            TestFixture(sourceType = "ID", requiredType = "IssueType"),

            // Different array cardinality
            TestFixture(sourceType = "[ID]", requiredType = "ID"),
            TestFixture(sourceType = "[ID!]", requiredType = "ID"),
            TestFixture(sourceType = "ID", requiredType = "[ID]"),
            TestFixture(sourceType = "ID", requiredType = "[[ID]!]"),
        ).map { (sourceType, requiredType) ->
            DynamicTest.dynamicTest("$sourceType cannot be assigned to $requiredType") {
                val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "jira" to """
                            type Query {
                                issueById(search: $requiredType): JiraIssue
                            }
                            input SearchQuery {
                                author: String
                            }
                            enum IssueType {
                                Story
                            }
                            type JiraIssue {
                                id: ID
                                related(myArg: $sourceType): JiraIssue @hydrated(
                                    field: "issueById"
                                    arguments: [{ name: "search", value: "$argument.myArg" }]
                                )
                            }
                        """.trimIndent(),
                    ),
                )

                // When
                val errors = validate(fixture)

                // Then
                assertTrue(errors.size == 1)

                val error = errors.singleOfType<NadelHydrationArgumentIncompatibleTypeError>()
                val hydrationArgument = error.hydrationArgument
                assertTrue(hydrationArgument.name == "search")
                assertTrue(hydrationArgument is NadelHydrationArgumentDefinition.FieldArgument)
                assertTrue(hydrationArgument.argumentName == "myArg")
                assertTrue(GraphQLTypeUtil.simplePrint(error.suppliedType) == sourceType)
                assertTrue(GraphQLTypeUtil.simplePrint(error.requiredType) == requiredType)
            }
        }
    }

    @TestFactory
    fun `batched hydration ignores type wrappings on supplied argument entirely`(): List<DynamicTest> {
        data class TestFixture(
            val sourceType: String,
            val requiredType: String,
        )

        return listOf(
            // Different array cardinality
            TestFixture(sourceType = "[ID]", requiredType = "[ID]!"),
            TestFixture(sourceType = "[ID!]", requiredType = "[ID]"),
            TestFixture(sourceType = "ID", requiredType = "[ID]"),
            TestFixture(sourceType = "ID", requiredType = "[ID]!"),
            TestFixture(sourceType = "ID", requiredType = "[String!]"),
        ).map { (sourceType, requiredType) ->
            DynamicTest.dynamicTest("$sourceType cannot be assigned to $requiredType") {
                val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "jira" to """
                            type Query {
                                issueById(ids: $requiredType): [JiraIssue]
                            }
                            input SearchQuery {
                                author: String
                            }
                            enum IssueType {
                                Story
                            }
                            type JiraIssue {
                                id: $sourceType
                                related: JiraIssue @hydrated(
                                    field: "issueById"
                                    arguments: [{ name: "ids", value: "$source.id" }]
                                )
                            }
                        """.trimIndent(),
                    ),
                )

                // When
                val errors = validate(fixture)

                // Then
                assertTrue(errors.isEmpty())
            }
        }
    }

    @TestFactory
    fun `batched hydration ignores type wrappings but checks unwrapped type`(): List<DynamicTest> {
        data class TestFixture(
            val sourceType: String,
            val requiredType: String,
        )

        return listOf(
            // Different array cardinality
            TestFixture(sourceType = "[ID]", requiredType = "[Boolean]!"),
            TestFixture(sourceType = "[ID!]", requiredType = "[Boolean]"),
            TestFixture(sourceType = "ID", requiredType = "[Int]"),
        ).map { (sourceType, requiredType) ->
            DynamicTest.dynamicTest("$sourceType cannot be assigned to $requiredType") {
                val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "jira" to """
                            type Query {
                                issueById(ids: $requiredType): [JiraIssue]
                            }
                            input SearchQuery {
                                author: String
                            }
                            enum IssueType {
                                Story
                            }
                            type JiraIssue {
                                id: $sourceType
                                related: JiraIssue @hydrated(
                                    field: "issueById"
                                    arguments: [{ name: "ids", value: "$source.id" }]
                                )
                            }
                        """.trimIndent(),
                    ),
                )

                // When
                val errors = validate(fixture)

                // Then
                assertTrue(errors.size == 1)

                val error = errors.singleOfType<NadelHydrationArgumentIncompatibleTypeError>()
                assertTrue(error.parentType.overall.name == "JiraIssue")
                assertTrue(error.virtualField.name == "related")
                assertTrue(error.hydration.backingField == listOf("issueById"))
                assertTrue(error.hydrationArgument.name == "ids")
                assertTrue(GraphQLTypeUtil.simplePrint(error.suppliedType) == sourceType.filter(Char::isLetter))
                assertTrue(GraphQLTypeUtil.simplePrint(error.requiredType) == requiredType.filter(Char::isLetter))
            }
        }
    }

    private fun NadelValidationTestFixture(overallSchema: Map<String, String>): NadelValidationTestFixture {
        return NadelValidationTestFixture(
            overallSchema = overallSchema,
            underlyingSchema = overallSchema
                .mapValues { (_, schema) ->
                    makeUnderlyingSchema(schema)
                },
        )
    }
}
