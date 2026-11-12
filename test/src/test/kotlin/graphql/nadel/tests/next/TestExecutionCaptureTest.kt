package graphql.nadel.tests.next

import graphql.incremental.DeferPayload
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.incremental.DelayedIncrementalPartialResultImpl
import graphql.language.AstPrinter
import graphql.nadel.NadelIncrementalServiceExecutionResult
import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.parser.Parser
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class TestExecutionCaptureTest {
    @Test
    fun `captures service results before mutation and returns the original result`() {
        // Given
        val capture = TestExecutionCapture()
        val query = "query Introspection(\$name: String!) { __type(name: \$name) { name } }"
        val result = NadelServiceExecutionResultImpl(
            data = mutableMapOf("__type" to null),
            errors = mutableListOf(
                mutableMapOf("message" to "Introspection unavailable", "path" to listOf("__type")),
            ),
            extensions = mutableMapOf("cache" to "hit"),
        )

        // When
        val returned = capture.capture(
            service = "__introspection",
            query = query,
            variables = mapOf("name" to "Query"),
            result = result,
        )
        result.data["__type"] = mapOf("name" to "Query")
        result.errors.single()!!["message"] = "Changed"
        result.extensions["cache"] = "changed"

        // Then
        assertSame(result, returned)
        assertEquals(
            listOf(
                TestExecutionCapture.Call(
                    service = "__introspection",
                    query = AstPrinter.printAst(Parser().parseDocument(query)),
                    variables = mapOf("name" to "Query"),
                    result = mapOf(
                        "data" to mapOf("__type" to null),
                        "errors" to listOf(
                            mapOf("message" to "Introspection unavailable", "path" to listOf("__type")),
                        ),
                        "extensions" to mapOf("cache" to "hit"),
                    ),
                    delayedResults = emptyList(),
                ),
            ),
            capture.calls,
        )
    }

    @Test
    fun `captures incremental service results without eagerly subscribing or changing payloads`() = runTest {
        // Given
        val capture = TestExecutionCapture()
        val subscriptions = AtomicInteger()
        val field = mutableMapOf("name" to "me")
        val initialPayload = DeferPayload.newDeferredItem()
            .path(listOf("__type"))
            .label("Name")
            .data(mapOf("name" to "Query"))
            .build()
        val delayedResult = DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
            .hasNext(false)
            .incrementalItems(
                listOf(
                    DeferPayload.newDeferredItem()
                        .path(listOf("__type"))
                        .label("Fields")
                        .data(mapOf("fields" to listOf(field)))
                        .build(),
                ),
            )
            .build()
        val result = NadelIncrementalServiceExecutionResult(
            data = mutableMapOf("__type" to emptyMap<String, Any?>()),
            incremental = listOf(initialPayload),
            incrementalItemPublisher = flow<DelayedIncrementalPartialResult> {
                subscriptions.incrementAndGet()
                emit(delayedResult)
            }.asPublisher(),
            hasNext = true,
        )

        // When
        val returned = capture.capture(
            service = "__introspection",
            query = """
                {
                  __type(name: "Query") {
                    ... @defer(label: "Name") { name }
                    ... @defer(label: "Fields") { fields { name } }
                  }
                }
            """.trimIndent(),
            variables = emptyMap(),
            result = result,
        )

        // Then
        val incrementalResult = assertIs<NadelIncrementalServiceExecutionResult>(returned)
        assertSame(result.data, incrementalResult.data)
        assertSame(result.errors, incrementalResult.errors)
        assertSame(result.extensions, incrementalResult.extensions)
        assertSame(result.incremental, incrementalResult.incremental)
        assertEquals(result.hasNext, incrementalResult.hasNext)
        assertEquals(0, subscriptions.get())
        assertEquals(emptyList(), capture.calls.single().delayedResults)
        assertEquals(
            mapOf(
                "data" to mapOf("__type" to emptyMap<String, Any?>()),
                "hasNext" to true,
                "incremental" to listOf(
                    mapOf("path" to listOf("__type"), "label" to "Name", "data" to mapOf("name" to "Query")),
                ),
            ),
            capture.calls.single().result,
        )

        // When
        val delayedResults = incrementalResult.incrementalItemPublisher.asFlow().toList()
        field["name"] = "changed"

        // Then
        assertEquals(1, subscriptions.get())
        assertSame(delayedResult, delayedResults.single())
        assertEquals(
            listOf(
                mapOf(
                    "hasNext" to false,
                    "incremental" to listOf(
                        mapOf(
                            "path" to listOf("__type"),
                            "label" to "Fields",
                            "data" to mapOf("fields" to listOf(mapOf("name" to "me"))),
                        ),
                    ),
                ),
            ),
            capture.calls.single().delayedResults,
        )
    }
}
