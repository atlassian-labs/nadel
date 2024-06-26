package graphql.nadel.engine

import graphql.incremental.DeferPayload
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.incremental.DelayedIncrementalPartialResultImpl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class NadelIncrementalResultSupportTest {
    @Test
    fun `channel closes once initial result comes in and there are no pending defer jobs`() {
        val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)
        val subject = NadelIncrementalResultSupport(channel)

        assertFalse(channel.isClosedForSend)
        assertFalse(channel.isClosedForReceive)

        // When
        subject.onInitialResultComplete()

        // Then
        assertTrue(channel.isClosedForSend)
        assertTrue(channel.isClosedForReceive)
    }

    @Test
    fun `after last job the hasNext is false`() = runTest {
        val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)

        val subject = NadelIncrementalResultSupport(channel)
        // Use locks to continue the deferred jobs when we release the lock
        val firstLock = Mutex(true)
        val secondLock = Mutex(true)
        val thirdLock = Mutex(true)

        // When
        subject.defer {
            firstLock.withLock {
                DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                    .incrementalItems(emptyList())
                    .hasNext(true)
                    .build()
            }
        }
        subject.defer {
            secondLock.withLock {
                DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                    .incrementalItems(emptyList())
                    .hasNext(true)
                    .build()
            }
        }
        subject.defer {
            // Wait until test tells us to continue
            thirdLock.withLock {
                DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                    .incrementalItems(
                        listOf(
                            DeferPayload.newDeferredItem()
                                .data("Bye world")
                                .path(listOf("echo"))
                                .build(),
                        ),
                    )
                    .hasNext(true)
                    .build()
            }
        }

        subject.onInitialResultComplete()

        // Then
        firstLock.unlock()

        val results = channel
            .consumeAsFlow()
            .withIndex()
            .onEach { (index, _) ->
                when (index) {
                    0 -> secondLock.unlock()
                    1 -> thirdLock.unlock()
                    2 -> {} // Do nothing
                    else -> throw IllegalArgumentException("Test does not expect this many elements")
                }
            }
            .map { (_, value) -> value }
            .toList()

        assertTrue(results.dropLast(n = 1).all { it.hasNext() })
        val lastResult = results.last()
        assertTrue((lastResult.incremental?.single() as DeferPayload).getData<String>() == "Bye world")
        assertFalse(lastResult.hasNext())
    }

    @Test
    fun `does not send anything before onInitialResultComplete is invoked`() = runTest {
        val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)

        val subject = NadelIncrementalResultSupport(channel)
        val lock = CompletableDeferred<Boolean>()

        // When
        subject.defer {
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(emptyList())
                .hasNext(true)
                .extensions(mapOf("hello" to "world"))
                .build()
                .also {
                    lock.complete(true)
                }
        }

        // Then
        lock.join()

        // Nothing comes out
        val timeoutResult = withTimeoutOrNull(100.milliseconds) {
            channel.receive()
        }
        assertTrue(timeoutResult == null)
        assertTrue(channel.isEmpty)

        // We receive the result once we invoke this
        subject.onInitialResultComplete()
        assertTrue(channel.receive().extensions == mapOf("hello" to "world"))
    }

    @Test
    fun `hasNext is true if last job launches more jobs`() = runTest {
        val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)
        val subject = NadelIncrementalResultSupport(channel)
        val firstLock = CompletableDeferred<Boolean>()
        val secondLock = CompletableDeferred<Boolean>()

        // When
        subject.defer {
            // Wait until test tells us to continue
            firstLock.join()

            subject.defer {
                secondLock.join()

                DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                    .incrementalItems(emptyList())
                    .extensions(mapOf("id" to 2))
                    .hasNext(true)
                    .build()
            }

            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(emptyList())
                .extensions(mapOf("id" to 1))
                .hasNext(false)
                .build()
        }

        // Then
        subject.onInitialResultComplete()
        firstLock.complete(true)

        val first = channel.receive()
        assertTrue(first.hasNext())
        assertTrue(first.extensions == mapOf("id" to 1))

        secondLock.complete(true)
        val second = channel.receive()
        assertFalse(second.hasNext())
        assertTrue(second.extensions == mapOf("id" to 2))
    }

    @Test
    fun `hasNext is true if there is another job still running`() = runTest {
        val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)

        val subject = NadelIncrementalResultSupport(channel)
        val firstLock = CompletableDeferred<Boolean>()
        val secondLock = CompletableDeferred<Boolean>()

        // When
        subject.defer {
            // Wait until test tells us to continue
            secondLock.join()

            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(
                    listOf(
                        DeferPayload.newDeferredItem()
                            .data("Hello world")
                            .path(listOf("echo"))
                            .build()
                    )
                )
                .hasNext(false)
                .build()
        }

        subject.defer {
            // Wait until test tells us to continue
            firstLock.join()

            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(emptyList())
                .hasNext(false)
                .build()
        }

        // Then
        subject.onInitialResultComplete()

        firstLock.complete(true)
        val firstItem = channel.receive()
        assertTrue(firstItem.incremental?.isEmpty() == true)
        assertTrue(firstItem.hasNext())

        secondLock.complete(true)
        val secondItem = channel.receive()
        assertTrue(secondItem !== firstItem)
        assertTrue(secondItem.incremental?.isNotEmpty() == true)
        assertFalse(secondItem.hasNext())
    }

    @Test
    fun `forwards responses from Flows`() = runTest {
        val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)

        val subject = NadelIncrementalResultSupport(channel)

        val lock = Mutex(locked = true)
        // When
        subject.defer(
            flow {
                lock.withLock {
                }

                emit(
                    DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                        .incrementalItems(emptyList())
                        .hasNext(true)
                        .extensions(mapOf("one" to true))
                        .build(),
                )
                emit(
                    DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                        .incrementalItems(emptyList())
                        .hasNext(false)
                        .extensions(mapOf("two" to true))
                        .build(),
                )
            },
        )
        subject.defer(
            flow {
                lock.withLock {
                }

                emit(
                    DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                        .incrementalItems(emptyList())
                        .hasNext(false)
                        .extensions(mapOf("three" to true))
                        .build(),
                )
            },
        )

        // Then
        subject.onInitialResultComplete()
        lock.unlock()

        val contents = channel.toList()
        assertTrue(contents.size == 3)
        assertTrue(contents.map { it.hasNext() } == listOf(true, true, false))

        val extensions = contents.fold(emptyMap<Any?, Any?>()) { acc, element ->
            acc + (element.extensions ?: emptyMap())
        }
        assertTrue(extensions == mapOf("one" to true, "two" to true, "three" to true))
    }

    @Test
    fun `channel completes even if a Flow failed`() {
        var completed = false
        try {
            runTest {
                val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)

                val subject = NadelIncrementalResultSupport(channel)

                val failureMutex = Mutex(true)

                // When
                subject.defer(
                    flow {
                        subject.defer(
                            flow {
                                failureMutex.unlock()
                                emit(
                                    DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                                        .incrementalItems(emptyList())
                                        .hasNext(false)
                                        .build(),
                                )
                            }
                        )

                        failureMutex.withLock {
                            throw UnsupportedOperationException()
                        }
                    },
                )

                // Then
                subject.onInitialResultComplete()

                val contents = channel.toList()
                assertTrue(contents.size == 1)
                // todo: we need to add error handling i.e. forward a GraphQL error in the delayed response with hasNext=false
                // assertTrue(contents.map { it.hasNext() } == listOf(false))

                completed = true
            }
        } catch (e: UnsupportedOperationException) {
            // Our exception Flow will cause runTest to fail at the very end, because a coroutine failed
            // The exception is expected, so we ignore it here, and just assert that the test actually finished
            assertTrue(completed)
        }
    }

    /**
     * todo: what actually happens here? this is not a well defined case right now
     */
    @Test
    fun `handles empty Flow`() = runTest {
        val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)

        val subject = NadelIncrementalResultSupport(channel)

        // When
        subject.defer(emptyFlow())

        // Then
        subject.onInitialResultComplete()

        val contents = channel.toList()
        assertTrue(contents.isEmpty())
    }

    @Test
    fun `Flow can launch more defer jobs`() = runTest {
        val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)

        val subject = NadelIncrementalResultSupport(channel)
        val childLock = Mutex(locked = true)

        // When
        subject.defer(
            flow {
                subject.defer {
                    childLock.withLock {
                        DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                            .incrementalItems(emptyList())
                            .hasNext(false)
                            .extensions(mapOf("child" to true))
                            .build()
                    }
                }

                emit(
                    DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                        .incrementalItems(emptyList())
                        .hasNext(false)
                        .extensions(mapOf("parent" to true))
                        .build(),
                )
            }
        )

        // Then
        subject.onInitialResultComplete()

        val parent = channel.receive()
        assertTrue(parent.hasNext())
        assertTrue(parent.extensions == mapOf("parent" to true))

        childLock.unlock()

        val child = channel.receive()
        assertFalse(child.hasNext())
        assertTrue(child.extensions == mapOf("child" to true))

        assertTrue(channel.toList().isEmpty())
    }

    @Test
    fun `errors if multiple elements in Flow are hasNext false`() {
        val exception = assertThrows<IllegalStateException> {
            runTest {
                val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)

                val subject = NadelIncrementalResultSupport(channel)

                // When
                subject.defer(
                    flowOf(
                        DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                            .incrementalItems(emptyList())
                            .hasNext(false)
                            .build(),
                        DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                            .incrementalItems(emptyList())
                            .hasNext(false)
                            .build(),
                    ),
                )

                // Then
                subject.onInitialResultComplete()

                val contents = channel.toList()
                assertTrue(contents.size == 1)
                assertTrue(contents.map { it.hasNext() } == listOf(false))
            }
        }

        assertTrue(exception.message == "Cannot close outstanding job more than once")
    }

    @Test
    fun `channel still closes if the last defer job fails`() {
        var testCompleted = false

        try {
            runTest {
                val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)

                val subject = NadelIncrementalResultSupport(channel)
                val lock = CompletableDeferred<Boolean>()

                assertFalse(channel.isClosedForSend)

                // When
                subject.defer {
                    // Wait until test tells us to continue
                    lock.join()
                    throw UnsupportedOperationException("Hello")
                }
                subject.onInitialResultComplete()

                // Then
                lock.complete(true)
                // Can only get last element once the channel is closed
                assertTrue(channel.consumeAsFlow().lastOrNull() == null)

                // Must be at end of runTest
                testCompleted = true
            }
        } catch (e: UnsupportedOperationException) {
            assertTrue(e.message == "Hello")
        }

        // Coroutines code will throw uncaught exceptions even though we threw it deliberately
        // Just ensure that we actually ran all the asserts and the tests is fine
        assertTrue(testCompleted)
    }
}
