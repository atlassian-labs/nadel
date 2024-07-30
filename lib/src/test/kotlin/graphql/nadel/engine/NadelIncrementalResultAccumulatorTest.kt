package graphql.nadel.engine

import graphql.execution.RawVariables
import graphql.execution.ResultPath
import graphql.incremental.DeferPayload
import graphql.incremental.DelayedIncrementalPartialResultImpl
import graphql.nadel.engine.util.JsonMap
import graphql.normalized.ExecutableNormalizedOperation
import graphql.normalized.ExecutableNormalizedOperationFactory
import graphql.normalized.ExecutableNormalizedOperationFactory.Options.defaultOptions
import graphql.parser.Parser
import graphql.schema.idl.SchemaGenerator
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NadelIncrementalResultAccumulatorTest {
    private val schema = SchemaGenerator.createdMockedSchema(
        // language=GraphQL
        """
            type Query {
                issue(id: ID!): Issue
                issues(ids: [ID!]!): [Issue]
            }
            type Issue {
                id: ID!
                key: String
                assignee: User
            }
            type User {
                id: ID!
                name: String
                workedOn: [Issue]
            }
        """.trimIndent(),
    )

    @Test
    fun `has no results if nothing is accumulated`() {
        val accumulator = makeAccumulator(
            query = """
                query {
                  issue(id: "1") {
                    ... @defer {
                      assignee {
                        name
                      }
                    }
                  }
                }
            """.trimIndent(),
        )

        // When
        val result = accumulator.getIncrementalPartialResult(true)

        // Then
        assertTrue(result == null)
    }

    @TestFactory
    fun `yields result once all fields are accumulated`(): List<DynamicTest> {
        data class TestCase(
            val hasNext: Boolean,
        )

        return listOf(
            TestCase(hasNext = true),
            TestCase(hasNext = false),
        ).map { testCase ->
            DynamicTest.dynamicTest(testCase.toString()) {
                val accumulator = makeAccumulator(
                    query = """
                        query {
                          issue(id: "1") {
                            ... @defer {
                              assignee {
                                name
                              }
                            }
                          }
                        }
                    """.trimIndent(),
                )

                // When
                accumulator.accumulate(
                    DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                        .incrementalItems(
                            listOf(
                                DeferPayload.newDeferredItem()
                                    .data(
                                        mapOf(
                                            "assignee" to mapOf(
                                                "name" to "Worker",
                                            ),
                                        ),
                                    )
                                    .path(ResultPath.parse("/issue"))
                                    .build(),
                            ),
                        )
                        .build(),
                )

                // Then
                val result = accumulator.getIncrementalPartialResult(testCase.hasNext)
                assertTrue(result != null)
                assertTrue(result.hasNext() == testCase.hasNext)
                assertTrue(result.incremental?.size == 1)

                val deferred = result.incremental!!.single()
                assertTrue(deferred is DeferPayload)
                assertTrue(deferred.path == listOf("issue"))
                assertTrue(deferred.getData<JsonMap>() == mapOf("assignee" to mapOf("name" to "Worker")))
            }
        }
    }

    @TestFactory
    fun `does not yield result if only part of payload is accumulated`(): List<DynamicTest> {
        data class TestCase(
            val payloadData: JsonMap,
        )

        return listOf(
            TestCase(
                payloadData = mapOf(
                    "assignee" to mapOf(
                        "name" to "Worker",
                    ),
                ),
            ),
            TestCase(
                payloadData = mapOf(
                    "id" to "issue/1",
                ),
            ),
            TestCase(
                payloadData = emptyMap(),
            ),
        ).map { testCase ->
            DynamicTest.dynamicTest(testCase.toString()) {
                val accumulator = makeAccumulator(
                    query = """
                        query {
                          issue(id: "1") {
                            ... @defer {
                              id
                              assignee {
                                name
                              }
                            }
                          }
                        }
                    """.trimIndent(),
                )

                // When
                accumulator.accumulate(
                    DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                        .incrementalItems(
                            listOf(
                                DeferPayload.newDeferredItem()
                                    .data(testCase.payloadData)
                                    .path(ResultPath.parse("/issue"))
                                    .build(),
                            ),
                        )
                        .build(),
                )

                // Then
                val result = accumulator.getIncrementalPartialResult(false)
                assertTrue(result == null)
            }
        }
    }

    @Test
    fun `yields list elements that are complete`() {
        val accumulator = makeAccumulator(
            query = """
                query {
                  issues(ids: ["1", "2"]) {
                    ... @defer {
                      id
                      assignee {
                        name
                      }
                    }
                  }
                }
            """.trimIndent(),
        )

        // When
        accumulator.accumulate(
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(
                    listOf(
                        DeferPayload.newDeferredItem()
                            .data(
                                mapOf(
                                    "id" to "issue/1",
                                    "assignee" to null, // Complete
                                ),
                            )
                            .path(
                                ResultPath.fromList(
                                    listOf("issues", 3),
                                ),
                            )
                            .build(),
                    ),
                )
                .build(),
        )
        accumulator.accumulate(
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(
                    listOf(
                        DeferPayload.newDeferredItem()
                            .data(
                                mapOf(
                                    "id" to "Sad", // Missing assignee
                                ),
                            )
                            .path(
                                ResultPath.fromList(
                                    listOf("issues", 0),
                                ),
                            )
                            .build(),
                    ),
                )
                .build(),
        )

        // Then
        val result = accumulator.getIncrementalPartialResult(true)
        assertTrue(result != null)
        assertTrue(result.hasNext())
        assertTrue(result.incremental?.size == 1)

        val deferred = result.incremental!!.single()
        assertTrue(deferred is DeferPayload)
        assertTrue(deferred.path == listOf("issues", 3))
        assertTrue(deferred.getData<JsonMap>() == mapOf("id" to "issue/1", "assignee" to null))
    }

    @Test
    fun `does not yield list payload if not ready`() {
        val accumulator = makeAccumulator(
            query = """
                query {
                  issues(ids: ["1", "2"]) {
                    ... @defer {
                      id
                      assignee {
                        name
                      }
                    }
                  }
                }
            """.trimIndent(),
        )

        // When
        accumulator.accumulate(
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(
                    listOf(
                        DeferPayload.newDeferredItem()
                            .data(
                                mapOf(
                                    "id" to "issue/1", // Missing assignee
                                ),
                            )
                            .path(
                                ResultPath.fromList(
                                    listOf("issues", 0),
                                ),
                            )
                            .build(),
                    ),
                )
                .build(),
        )

        // Then
        val result = accumulator.getIncrementalPartialResult(true)
        assertTrue(result == null)
    }

    @Test
    fun `accumulates separate defer payloads`() {
        val accumulator = makeAccumulator(
            query = """
                query {
                  issue(id: 1) {
                    ... @defer {
                      id
                    }
                  }
                  issues(ids: ["1", "2"]) {
                    id
                    assignee {
                      ... @defer {
                        id
                        name
                      }
                    }
                  }
                }
            """.trimIndent(),
        )

        // When
        accumulator.accumulate(
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(
                    listOf(
                        DeferPayload.newDeferredItem()
                            .data(
                                mapOf(
                                    "name" to "Hello", // Missing id
                                ),
                            )
                            .path(
                                ResultPath.fromList(
                                    listOf("issues", 1, "assignee"),
                                ),
                            )
                            .build(),
                        DeferPayload.newDeferredItem()
                            .data(
                                mapOf(
                                    "id" to "issue/10",
                                ),
                            )
                            .path(
                                ResultPath.fromList(
                                    listOf("issue"),
                                ),
                            )
                            .build(),
                    ),
                )
                .build(),
        )

        // Then
        val result1 = accumulator.getIncrementalPartialResult(true)
        assertTrue(result1 != null)
        assertTrue(result1.hasNext())
        assertTrue(result1.incremental?.size == 1)

        val issuePayload = result1.incremental?.single()
        assertTrue(issuePayload is DeferPayload)
        assertTrue(issuePayload.path == listOf("issue"))
        assertTrue(issuePayload.getData<JsonMap>() == mapOf("id" to "issue/10"))

        // When
        accumulator.accumulate(
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(
                    listOf(
                        DeferPayload.newDeferredItem()
                            .data(
                                mapOf(
                                    "id" to "user/10",
                                ),
                            )
                            .path(
                                ResultPath.fromList(
                                    listOf("issues", 1, "assignee"),
                                ),
                            )
                            .build(),
                    ),
                )
                .build(),
        )

        // Then
        val result2 = accumulator.getIncrementalPartialResult(false)
        assertTrue(result2 != null)
        assertFalse(result2.hasNext())
        assertTrue(result2.incremental?.size == 1)

        val assigneePayload = result2.incremental?.single()
        assertTrue(assigneePayload is DeferPayload)
        assertTrue(assigneePayload.path == listOf("issues", 1, "assignee"))
        assertTrue(assigneePayload.getData<JsonMap>() == mapOf("id" to "user/10", "name" to "Hello"))
    }

    @Test
    fun `uses label to identify defer payload`() {
        val accumulator = makeAccumulator(
            query = """
                query {
                  issue(id: 1) {
                    ... @defer(label: "wow") {
                      key
                    }
                    ... @defer(label: "hot") {
                      key
                    }
                    ... @defer {
                      key
                    }
                  }
                }
            """.trimIndent(),
        )

        // When
        accumulator.accumulate(
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(
                    listOf(
                        DeferPayload.newDeferredItem()
                            .data(
                                mapOf(
                                    "key" to "HOT-123",
                                ),
                            )
                            .path(
                                ResultPath.fromList(
                                    listOf("issue"),
                                ),
                            )
                            .label("hot")
                            .build(),
                        DeferPayload.newDeferredItem()
                            .data(
                                mapOf(
                                    "key" to "yowza",
                                ),
                            )
                            .path(
                                ResultPath.fromList(
                                    listOf("issue"),
                                ),
                            )
                            .label("wow")
                            .build(),
                    ),
                )
                .build(),
        )
        accumulator.accumulate(
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(
                    listOf(
                        DeferPayload.newDeferredItem()
                            .data(
                                mapOf(
                                    "key" to "boring",
                                ),
                            )
                            .path(
                                ResultPath.fromList(
                                    listOf("issue"),
                                ),
                            )
                            .build(),
                    ),
                )
                .build(),
        )

        // Then
        val result = accumulator.getIncrementalPartialResult(true)
        assertTrue(result != null)
        assertTrue(result.incremental?.size == 3)

        val labelAndKey = result.incremental!!
            .mapTo(LinkedHashSet()) {
                it.label to (it as DeferPayload).getData<JsonMap>()!!["key"]
            }

        val expectedLabelAndKeys = setOf(
            "hot" to "HOT-123",
            "wow" to "yowza",
            null to "boring",
        )

        assertTrue(labelAndKey == expectedLabelAndKeys)
    }

    @Test
    fun `defer payload with matching path but wrong label does not accumulate`() {
        val accumulator = makeAccumulator(
            query = """
                query {
                  issue(id: 1) {
                    ... @defer {
                      id
                    }
                  }
                  issues(ids: ["1", "2"]) {
                    id
                    assignee {
                      ... @defer {
                        id
                        name
                      }
                    }
                  }
                }
            """.trimIndent(),
        )

        // When
        accumulator.accumulate(
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(
                    listOf(
                        DeferPayload.newDeferredItem()
                            .data(
                                mapOf(
                                    "name" to "Hello", // Missing id
                                ),
                            )
                            .path(
                                ResultPath.fromList(
                                    listOf("issues", 1, "assignee"),
                                ),
                            )
                            .label("hello world")
                            .build(),
                    ),
                )
                .build(),
        )

        // Then
        val result = accumulator.getIncrementalPartialResult(true)
        assertTrue(result == null)
    }

    @Test
    fun `defer payload with matching path but no label does not accumulate`() {
        val accumulator = makeAccumulator(
            query = """
                query {
                  issues(ids: ["1", "2"]) {
                    id
                    assignee {
                      ... @defer(label: "worker") {
                        id
                        name
                      }
                    }
                  }
                }
            """.trimIndent(),
        )

        // When
        accumulator.accumulate(
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(
                    listOf(
                        DeferPayload.newDeferredItem()
                            .data(
                                mapOf(
                                    "name" to "Hello", // Missing id
                                ),
                            )
                            .path(
                                ResultPath.fromList(
                                    listOf("issues", 1, "assignee"),
                                ),
                            )
                            .build(),
                    ),
                )
                .build(),
        )

        // Then
        val result = accumulator.getIncrementalPartialResult(true)
        assertTrue(result == null)
    }

    @Test
    fun `accumulate payload comes in on unknown defer path`() {
        val accumulator = makeAccumulator(
            query = """
                query {
                  issues(ids: ["1", "2"]) {
                    id
                    assignee {
                      ... @defer(label: "worker") {
                        name
                      }
                    }
                  }
                }
            """.trimIndent(),
        )

        // When
        accumulator.accumulate(
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(
                    listOf(
                        DeferPayload.newDeferredItem()
                            .data(
                                mapOf(
                                    "name" to "Hello",
                                ),
                            )
                            .path(
                                ResultPath.fromList(
                                    listOf("issues", 1, "owner"),
                                ),
                            )
                            .build(),
                    ),
                )
                .build(),
        )

        // Then
        val result = accumulator.getIncrementalPartialResult(true)
        assertTrue(result == null)
    }

    @Test
    fun `only yields accumulated payload once`() {
        val accumulator = makeAccumulator(
            query = """
                query {
                  issues(ids: ["1", "2"]) {
                    id
                    assignee {
                      ... @defer(label: "worker") {
                        name
                      }
                    }
                  }
                }
            """.trimIndent(),
        )

        // When
        accumulator.accumulate(
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(
                    listOf(
                        DeferPayload.newDeferredItem()
                            .data(
                                mapOf(
                                    "name" to "Hello",
                                ),
                            )
                            .path(
                                ResultPath.fromList(
                                    listOf("issues", 1, "assignee"),
                                ),
                            )
                            .label("worker")
                            .build(),
                    ),
                )
                .build(),
        )

        // Then
        val result = accumulator.getIncrementalPartialResult(true)
        assertTrue(result != null)
        assertTrue(result.incremental?.single()?.path == listOf("issues", 1, "assignee"))

        assertTrue(accumulator.getIncrementalPartialResult(true) == null)
        assertTrue(accumulator.getIncrementalPartialResult(true) == null)
    }

    @Test
    fun `yields multiple payloads in one go if stored up`() {
        val accumulator = makeAccumulator(
            query = """
                query {
                  issue(id: 1) {
                    ... @defer {
                      id
                    }
                    assignee {
                      ... @defer {
                        name
                      }
                    }
                  }
                }
            """.trimIndent(),
        )

        // When
        accumulator.accumulate(
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(
                    listOf(
                        DeferPayload.newDeferredItem()
                            .data(
                                mapOf(
                                    "id" to "issue/1",
                                ),
                            )
                            .path(ResultPath.parse("/issue"))
                            .build(),
                    ),
                )
                .build(),
        )
        accumulator.accumulate(
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(
                    listOf(
                        DeferPayload.newDeferredItem()
                            .data(
                                mapOf(
                                    "name" to "Someone",
                                ),
                            )
                            .path(ResultPath.parse("/issue/assignee"))
                            .build(),
                    ),
                )
                .build(),
        )

        // Then
        val result = accumulator.getIncrementalPartialResult(true)
        assertTrue(result != null)
        assertTrue(result.hasNext())
        assertTrue(result.incremental?.size == 2)

        val (idItem, nameItem) = result.incremental!!.sortedBy { it.path.size }

        assertTrue(idItem is DeferPayload)
        assertTrue(nameItem is DeferPayload)

        assertTrue(idItem.path == listOf("issue"))
        assertTrue(idItem.getData<JsonMap>() == mapOf("id" to "issue/1"))

        assertTrue(nameItem.path == listOf("issue", "assignee"))
        assertTrue(nameItem.getData<JsonMap>() == mapOf("name" to "Someone"))
    }

    @Test
    fun `can accumulate top level fields`() {
        val accumulator = makeAccumulator(
            query = """
                query {
                  ... @defer {
                    issue(id: 1) {
                      id
                    }
                  }
                }
            """.trimIndent(),
        )

        // When
        accumulator.accumulate(
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(
                    listOf(
                        DeferPayload.newDeferredItem()
                            .data(
                                mapOf(
                                    "issue" to mapOf(
                                        "id" to "1",
                                    ),
                                ),
                            )
                            .path(ResultPath.rootPath())
                            .build(),
                    ),
                )
                .build(),
        )

        // Then
        val result = accumulator.getIncrementalPartialResult(false)
        assertTrue(result != null)
        assertFalse(result.hasNext())
        assertTrue(result.incremental?.size == 1)

        val deferred = result.incremental!!.single()

        assertTrue(deferred is DeferPayload)
        assertTrue(deferred.path.isEmpty())
        assertTrue(deferred.getData<JsonMap>() == mapOf("issue" to mapOf("id" to "1")))
    }

    private fun makeExecutableNormalizedOperation(
        @Language("GraphQL")
        document: String,
        variables: JsonMap,
    ): ExecutableNormalizedOperation {
        return ExecutableNormalizedOperationFactory.createExecutableNormalizedOperationWithRawVariables(
            schema,
            Parser().parseDocument(document),
            null,
            RawVariables.of(variables),
            defaultOptions().deferSupport(true),
        )
    }

    private fun makeAccumulator(
        @Language("GraphQL")
        query: String,
        variables: JsonMap = emptyMap(),
    ): NadelIncrementalResultAccumulator {
        return NadelIncrementalResultAccumulator(
            operation = makeExecutableNormalizedOperation(
                document = query,
                variables = variables,
            ),
        )
    }
}
