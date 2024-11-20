package graphql.nadel.validation

import graphql.nadel.util.makeUnderlyingSchema
import graphql.schema.GraphQLTypeUtil
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertTrue

private const val source = "\$source"
private const val argument = "\$argument"

class NadelVirtualTypeValidationTest {
    @Test
    fun `passes if virtual types are valid`() {
        // Given
        val fixture = makeFixture(
            overallSchema = mapOf(
                "serviceA" to /*language=GraphQL*/ """
                    type Query {
                        echo: String
                        virtualField: DataView
                            @hydrated(
                                field: "data"
                                arguments: [{name: "id", value: "1"}]
                            )
                    }
                    type DataView @virtualType {
                        id: ID
                        string: String
                        int: Int
                        other: OtherDataView
                    }
                    type OtherDataView @virtualType {
                        boolean: Boolean
                        data: DataView
                    }
                """.trimIndent(),
                "serviceB" to /*language=GraphQL*/ """
                    type Query {
                      data(id: ID!): Data
                    }
                    type Data {
                      id: ID
                      string: String
                      bool: Boolean
                      int: Int!
                      other: OtherData
                    }
                    type OtherData {
                      boolean: Boolean!
                      data: Data
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isEmpty())
    }

    @TestFactory
    fun `returns validation error if field type is different`(): List<DynamicTest> {
        return listOf(
            "Boolean", // Completely different types are banned
            "String",
            "[[String]]",
            "[String]",
            "[String]!",
            "[Boolean!]",
            "[Boolean]",
            "Int!", // Must be nullable as backing data can be null
            "[Int]", // Do not allow any sort of list changes
            "[Int!]",
            "[Int!]!",
            "[Int]!",
        ).map { illegalType ->
            DynamicTest.dynamicTest(illegalType) {
                // Given
                val fixture = makeFixture(
                    overallSchema = mapOf(
                        "serviceA" to /*language=GraphQL*/ """
                            type Query {
                              echo: String
                              virtualField: DataView
                              @hydrated(
                                field: "data"
                                arguments: [{name: "id", value: "1"}]
                              )
                            }
                            type DataView @virtualType {
                              id: ID
                              string: String
                              int: $illegalType
                              other: OtherDataView
                            }
                            type OtherDataView @virtualType {
                              boolean: Int
                              data: DataView
                            }
                        """.trimIndent(),
                        "serviceB" to /*language=GraphQL*/ """
                            type Query {
                              data(id: ID!): Data
                            }
                            type Data {
                              id: ID
                              string: String
                              bool: Boolean
                              int: Int
                              other: OtherData
                            }
                            type OtherData {
                              boolean: Boolean
                              data: Data
                            }
                        """.trimIndent(),
                    ),
                )

                // When
                val errors = validate(fixture)

                // Then
                assertTrue(errors.isNotEmpty())
                val outputTypeErrors = errors
                    .asSequence()
                    .filterIsInstance<NadelVirtualTypeIncompatibleFieldOutputTypeError>()

                val dataViewError = outputTypeErrors.first { it.parent.overall.name == "DataView" }
                assertTrue(dataViewError.virtualField.name == "int")
                assertTrue(GraphQLTypeUtil.simplePrint(dataViewError.virtualField.type) == illegalType)
                assertTrue(dataViewError.backingField.name == "int")
                assertTrue(GraphQLTypeUtil.simplePrint(dataViewError.backingField.type) == "Int")

                val otherDataViewError = outputTypeErrors.first { it.parent.overall.name == "OtherDataView" }
                assertTrue(otherDataViewError.virtualField.name == "boolean")
                assertTrue(GraphQLTypeUtil.simplePrint(otherDataViewError.virtualField.type) == "Int")
                assertTrue(otherDataViewError.backingField.name == "boolean")
                assertTrue(GraphQLTypeUtil.simplePrint(otherDataViewError.backingField.type) == "Boolean")
            }
        }
    }

    @Test
    fun `returns validation error if unexpected field is present`() {
        // Given
        val fixture = makeFixture(
            overallSchema = mapOf(
                "serviceA" to /*language=GraphQL*/ """
                    type Query {
                      echo: String
                      virtualField: DataView
                      @hydrated(
                        field: "data"
                        arguments: [{name: "id", value: "1"}]
                      )
                    }
                    type DataView @virtualType {
                      id: ID
                      string: String
                      int: Int
                      other: OtherDataView
                    }
                    type OtherDataView @virtualType {
                      boolean: Int
                      data: DataView
                    }
                """.trimIndent(),
                "serviceB" to /*language=GraphQL*/ """
                    type Query {
                      data(id: ID!): Data
                    }
                    type Data {
                      id: ID
                      string: String
                      bool: Boolean
                      other: OtherData
                    }
                    type OtherData {
                      data: Data
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isNotEmpty())
        val missingBackingFieldErrors = errors
            .asSequence()
            .filterIsInstance<NadelVirtualTypeMissingBackingFieldError>()

        val dataViewError = missingBackingFieldErrors.first { it.type.overall.name == "DataView" }
        assertTrue(dataViewError.virtualField.name == "int")

        val otherDataViewError = missingBackingFieldErrors.first { it.type.overall.name == "OtherDataView" }
        assertTrue(otherDataViewError.virtualField.name == "boolean")
    }

    @Test
    fun `returns validation error if unexpected argument is present`() {
        // Given
        val fixture = makeFixture(
            overallSchema = mapOf(
                "serviceA" to /*language=GraphQL*/ """
                    type Query {
                      echo: String
                      virtualField: DataView
                      @hydrated(
                        field: "data"
                        arguments: [{name: "id", value: "1"}]
                      )
                    }
                    type DataView @virtualType {
                      id: ID
                      string(secret: Boolean): String
                      other: OtherDataView
                    }
                    type OtherDataView @virtualType {
                      data(secret: Boolean): DataView
                    }
                """.trimIndent(),
                "serviceB" to /*language=GraphQL*/ """
                    type Query {
                      data(id: ID!): Data
                    }
                    type Data {
                      id: ID
                      string(secret: Boolean): String
                      bool: Boolean
                      other(protected: Boolean): OtherData
                    }
                    type OtherData {
                      data: Data
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isNotEmpty())
        val missingBackingFieldArgumentErrors = errors
            .asSequence()
            .filterIsInstance<NadelVirtualTypeMissingBackingFieldArgumentError>()

        assertTrue(missingBackingFieldArgumentErrors.count() == 1)

        val dataViewError = missingBackingFieldArgumentErrors.single { it.type.overall.name == "OtherDataView" }
        assertTrue(dataViewError.virtualField.name == "data")
        assertTrue(dataViewError.virtualFieldArgument.name == "secret")
    }

    @Test
    fun `returns validation error if argument type is different`() {
        // Given
        val fixture = makeFixture(
            overallSchema = mapOf(
                "serviceA" to /*language=GraphQL*/ """
                    type Query {
                      echo: String
                      virtualField: DataView
                      @hydrated(
                        field: "data"
                        arguments: [{name: "id", value: "1"}]
                      )
                    }
                    type DataView @virtualType {
                      id: ID
                      string(secret: Boolean): String
                      other: OtherDataView
                    }
                    type OtherDataView @virtualType {
                      data(password: String): DataView
                    }
                """.trimIndent(),
                "serviceB" to /*language=GraphQL*/ """
                    type Query {
                      data(id: ID!): Data
                    }
                    type Data {
                      id: ID
                      string(secret: Boolean): String
                      bool: Boolean
                      other(protected: Boolean): OtherData
                    }
                    type OtherData {
                      data(password: Int): Data
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isNotEmpty())
        val incompatibleFieldArgumentErrors = errors
            .asSequence()
            .filterIsInstance<NadelVirtualTypeIncompatibleFieldArgumentError>()

        assertTrue(incompatibleFieldArgumentErrors.count() == 1)

        val dataViewError = incompatibleFieldArgumentErrors.single { it.type.overall.name == "OtherDataView" }
        assertTrue(dataViewError.virtualField.name == "data")
        assertTrue(dataViewError.virtualFieldArgument.name == "password")
    }

    @Test
    fun `returns validation error if type is an interface`() {
        // Given
        val fixture = makeFixture(
            overallSchema = mapOf(
                "serviceA" to /*language=GraphQL*/ """
                    directive @virtualType on OBJECT | INTERFACE
                    type Query {
                      echo: String
                      virtualField: DataView
                      @hydrated(
                        field: "data"
                        arguments: [{name: "id", value: "1"}]
                      )
                    }
                    type DataView @virtualType {
                      id: ID
                      string(secret: Boolean): String
                      other: OtherDataView
                    }
                    interface OtherDataView @virtualType {
                      data: DataView
                    }
                    type OtherDataViewImpl implements OtherDataView @virtualType {
                      data: DataView
                    }
                """.trimIndent(),
                "serviceB" to /*language=GraphQL*/ """
                    type Query {
                      data(id: ID!): Data
                    }
                    type Data {
                      id: ID
                      string(secret: Boolean): String
                      bool: Boolean
                      other(protected: Boolean): OtherData
                    }
                    interface OtherData {
                      data: Data
                    }
                    type OtherDataImpl implements OtherData {
                      data: Data
                    }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isNotEmpty())
        val illegalTypeError = errors
            .asSequence()
            .filterIsInstance<NadelVirtualTypeIllegalTypeError>()
            .single()

        assertTrue(illegalTypeError.type.overall.name == "OtherDataView")
    }

    @Test
    fun `can reference `() {
        // Given
        val fixture = makeFixture(
            overallSchema = mapOf(
                "graphStore" to /*language=GraphQL*/ """
                  type Query {
                    graphStore_query(
                      query: String!
                      first: Int
                      after: String
                    ): GraphStoreQueryConnection
                  }
                  type GraphStoreQueryConnection {
                    edges: [GraphStoreQueryEdge]
                    pageInfo: PageInfo
                  }
                  type GraphStoreQueryEdge {
                    nodeId: ID
                    cursor: String
                  }
                  type PageInfo {
                    hasNextPage: Boolean!
                    hasPreviousPage: Boolean!
                    startCursor: String
                    endCursor: String
                  }
                """.trimIndent(),
                "jiraIssue" to /*language=GraphQL*/ """
                  type Query {
                    issuesByIds(ids: [ID!]!): [JiraIssue]
                  }
                  type JiraIssue {
                    id: ID!
                    key: String!
                    title: String!
                  }
                """.trimIndent(),
                "businessReport" to /*language=GraphQL*/ """
                  type Query {
                    businessReport_findRecentWorkByTeam(
                      teamId: ID!
                      first: Int
                      after: String
                    ): WorkConnection
                    @hydrated(
                      service: "graph_store",
                      field: "graphStore_query"
                      arguments: [
                        {
                          name: "query"
                          value: "SELECT * FROM Work WHERE teamId = ?"
                        }
                        {
                          name: "first"
                          value: "$argument.first"
                        }
                        {
                          name: "after"
                          value: "$argument.after"
                        }
                      ]
                    )
                  }
                  type WorkConnection @virtualType {
                    edges: [WorkEdge]
                    pageInfo: PageInfo
                  }
                  type WorkEdge @virtualType {
                    nodeId: ID @hidden
                    node: WorkNode
                    @hydrated(
                      service: "jira"
                      field: "issuesByIds"
                      arguments: [{name: "ids", value: "$source.nodeId"}]
                    )
                    cursor: String
                  }
                  union WorkNode = JiraIssue
                """.trimIndent(),
            ),
            underlyingSchema = mapOf(
                "businessReport" to /*language=GraphQL*/ """
                  type Query {
                    echo: String
                  }
                """.trimIndent(),
            ),
        )

        // When
        val errors = validate(fixture)

        // Then
        assertTrue(errors.isEmpty())
    }

    private fun makeFixture(
        overallSchema: Map<String, String>,
        underlyingSchema: Map<String, String> = emptyMap(),
    ): NadelValidationTestFixture {
        return NadelValidationTestFixture(
            overallSchema = overallSchema,
            underlyingSchema = overallSchema
                .mapValues { (service, schema) ->
                    underlyingSchema[service] ?: makeUnderlyingSchema(schema)
                },
        )
    }
}
