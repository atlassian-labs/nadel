package graphql.nadel.tests.next.fixtures.batching

import graphql.nadel.Service
import graphql.nadel.hints.NadelBatchNamespacedRootFieldsHint
import graphql.nadel.hints.NadelBatchRootFieldsHint
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NamespacedRootBatchingTest {
    @Test
    fun `namespace and ordinary batching require their own global and service opt ins`() = runTest {
        val namespaces = listOf(
            "first: jira { issue(cloudId: \"one\") }",
            "second: jira { issue(cloudId: \"one\") }",
        )
        val roots = listOf(
            "rename__flatOne__issue: issue(cloudId: \"one\")",
            "rename__flatTwo__issue: issue(cloudId: \"one\")",
        )
        for (rootGlobal in listOf(false, true)) {
            for (rootService in listOf(false, true)) {
                for (namespaceGlobal in listOf(false, true)) {
                    for (namespaceService in listOf(false, true)) {
                        val capture = executeNamespacedQuery(
                            query = """
                                {
                                    ${namespaces.joinToString("\n")}
                                    flatOne: jira_issue(cloudId: "one")
                                    flatTwo: jira_issue(cloudId: "one")
                                }
                            """.trimIndent(),
                            rootHint = object : NadelBatchRootFieldsHint {
                                override fun invoke(): Boolean = rootGlobal
                                override fun invoke(service: Service): Boolean = rootService
                            },
                            namespaceHint = object : NadelBatchNamespacedRootFieldsHint {
                                override fun invoke(): Boolean = namespaceGlobal
                                override fun invoke(service: Service): Boolean = namespaceService
                            },
                        )

                        val batchRoots = rootGlobal && rootService
                        val batchNamespaces = namespaceGlobal && namespaceService
                        val selections = if (batchRoots && batchNamespaces) {
                            listOf((namespaces + roots).joinToString("\n"))
                        } else {
                            (if (batchNamespaces) listOf(namespaces.joinToString("\n")) else namespaces) +
                                (if (batchRoots) listOf(roots.joinToString("\n")) else roots)
                        }
                        assertBatchingQueries(capture, *selections.map { "{ $it }" }.toTypedArray())
                        assertBatchingData(
                            capture,
                            mapOf(
                                "first" to mapOf("issue" to "one"),
                                "second" to mapOf("issue" to "one"),
                                "flatOne" to "one",
                                "flatTwo" to "one",
                            ),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `namespace batching defaults off without changing ordinary batches`() = runTest {
        val capture = executeNamespacedQuery(
            query = """
                {
                    first: jira { issue }
                    second: jira { issue }
                    flatOne: jira_issue
                    flatTwo: jira_issue
                }
            """.trimIndent(),
            namespaceHint = null,
        )

        assertBatchingQueries(
            capture,
            "{ first: jira { issue } }",
            "{ second: jira { issue } }",
            "{ rename__flatOne__issue: issue rename__flatTwo__issue: issue }",
        )
        assertBatchingData(
            capture,
            mapOf(
                "first" to mapOf("issue" to "unsharded"),
                "second" to mapOf("issue" to "unsharded"),
                "flatOne" to "unsharded",
                "flatTwo" to "unsharded",
            ),
        )
    }

    @Test
    fun `namespace only requests batch with ordinary batching disabled`() = runTest {
        val capture = executeNamespacedQuery(
            query = "{ first: jira { issue } second: jira { issue } }",
            rootHint = NadelBatchRootFieldsHint { false },
        )

        assertBatchingQueries(capture, "{ first: jira { issue } second: jira { issue } }")
        assertBatchingData(
            capture,
            mapOf("first" to mapOf("issue" to "unsharded"), "second" to mapOf("issue" to "unsharded")),
        )
    }

    @Test
    fun `child shard keys isolate different shards mixed targets and unsharded fields`() = runTest {
        val capture = executeNamespacedQuery(
            query = """
                {
                    one: jira { value: issue(cloudId: "one") }
                    oneAgain: jira { value: issue(cloudId: "one") }
                    two: jira { value: issue(cloudId: "two") }
                    free: jira { value: issue }
                    freeAgain: jira { value: issue }
                    mixed: jira { bound: issue(cloudId: "one") free: issue }
                    different: jira { one: issue(cloudId: "one") two: issue(cloudId: "two") }
                    flatOne: jira_issue(cloudId: "one")
                    flatTwo: jira_issue(cloudId: "two")
                    flatFree: jira_issue
                }
            """.trimIndent(),
        )

        assertBatchingQueries(
            capture,
            """
                {
                    one: jira { value: issue(cloudId: "one") }
                    oneAgain: jira { value: issue(cloudId: "one") }
                    rename__flatOne__issue: issue(cloudId: "one")
                }
            """.trimIndent(),
            "{ two: jira { value: issue(cloudId: \"two\") } rename__flatTwo__issue: issue(cloudId: \"two\") }",
            "{ free: jira { value: issue } freeAgain: jira { value: issue } rename__flatFree__issue: issue }",
            "{ mixed: jira { bound: issue(cloudId: \"one\") free: issue } }",
            "{ different: jira { one: issue(cloudId: \"one\") two: issue(cloudId: \"two\") } }",
        )
        assertBatchingData(
            capture,
            mapOf(
                "one" to mapOf("value" to "one"),
                "oneAgain" to mapOf("value" to "one"),
                "two" to mapOf("value" to "two"),
                "free" to mapOf("value" to "unsharded"),
                "freeAgain" to mapOf("value" to "unsharded"),
                "mixed" to mapOf("bound" to "one", "free" to "unsharded"),
                "different" to mapOf("one" to "one", "two" to "two"),
                "flatOne" to "one",
                "flatTwo" to "two",
                "flatFree" to "unsharded",
            ),
        )
    }

    @Test
    fun `mixed batches preserve repeated child aliases fragments and skipped selections`() = runTest {
        val capture = executeNamespacedQuery(
            query = """
                {
                    first: jira { ...IssueFields }
                    second: jira {
                        same: issue(cloudId: "one") @include(if: true)
                        skipped: issue(cloudId: "two") @skip(if: true)
                        excluded: issue(cloudId: "two") @include(if: false)
                    }
                    empty: jira { issue @skip(if: true) }
                    same: jira_issue(cloudId: "one")
                    skippedRoot: jira_issue(cloudId: "two") @skip(if: true)
                }
                fragment IssueFields on JiraQuery {
                    same: issue(cloudId: "one")
                }
            """.trimIndent(),
        )

        assertBatchingQueries(
            capture,
            """
                {
                    first: jira { same: issue(cloudId: "one") }
                    second: jira { same: issue(cloudId: "one") }
                    rename__same__issue: issue(cloudId: "one")
                }
            """.trimIndent(),
        )
        assertBatchingData(
            capture,
            mapOf(
                "first" to mapOf("same" to "one"),
                "second" to mapOf("same" to "one"),
                "empty" to emptyMap<String, Any?>(),
                "same" to "one",
            ),
        )
    }

    @Test
    fun `partial errors keep namespace alias paths and healthy fields in the same batch`() = runTest {
        val capture = executeNamespacedQuery(
            query = """
                {
                    first: jira { same: issue(cloudId: "one", fail: true) }
                    second: jira { same: issue(cloudId: "one") }
                    same: jira_issue(cloudId: "one")
                }
            """.trimIndent(),
        )

        assertBatchingQueries(
            capture,
            """
                {
                    first: jira { same: issue(cloudId: "one", fail: true) }
                    second: jira { same: issue(cloudId: "one") }
                    rename__same__issue: issue(cloudId: "one")
                }
            """.trimIndent(),
        )
        val result = requireNotNull(capture.result)
        assertEquals(
            mapOf("first" to mapOf("same" to null), "second" to mapOf("same" to "one"), "same" to "one"),
            result.getData(),
        )
        assertEquals(listOf("first", "same"), result.errors.single().path)
        assertEquals("Issue lookup failed", result.errors.single().message)
    }
}
