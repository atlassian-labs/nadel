package graphql.nadel.engine

import graphql.incremental.DeferPayload
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.incremental.DelayedIncrementalPartialResultImpl
import graphql.nadel.test.mock
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verifyAll
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class NadelIncrementalResultSupportTest {
    private val accumulator = mock<NadelIncrementalResultAccumulator>()

    @AfterEach
    fun after() {
        confirmVerified(accumulator)
    }

    @Test
    fun `channel closes once initial result comes in and there are no pending defer jobs`() {
        val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)
        val subject = NadelIncrementalResultSupport(lazy { accumulator }, channel)

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

        val subject = NadelIncrementalResultSupport(lazy { accumulator }, channel)
        // Use locks to continue the deferred jobs when we release the lock
        val firstLock = Mutex(true)
        val secondLock = Mutex(true)
        val thirdLock = Mutex(true)

        every {
            accumulator.accumulate(any())
        } returns Unit
        every {
            accumulator.getIncrementalPartialResult(any())
        } answers {
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(emptyList())
                .hasNext(firstArg())
                .build()
        }

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
            thirdLock.withLock {
                DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                    .incrementalItems(emptyList())
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

        assertTrue(results.size == 3)
        assertTrue(results.dropLast(n = 1).all { it.hasNext() })
        val lastResult = results.last()
        assertFalse(lastResult.hasNext())

        verifyOrder {
            accumulator.accumulate(any())
            accumulator.getIncrementalPartialResult(true)
            accumulator.accumulate(any())
            accumulator.getIncrementalPartialResult(true)
            accumulator.accumulate(any())
            accumulator.getIncrementalPartialResult(false)
        }
    }

    @Test
    fun `does not send anything before onInitialResultComplete is invoked`() = runTest {
        val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)

        val subject = NadelIncrementalResultSupport(lazy { accumulator }, channel)
        val lock = CompletableDeferred<Boolean>()

        every {
            accumulator.accumulate(any())
        } returns Unit
        every {
            accumulator.getIncrementalPartialResult(any())
        } answers {
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(emptyList())
                .hasNext(firstArg())
                .extensions(mapOf("hello" to "world"))
                .build()
        }

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

        verifyOrder {
            accumulator.accumulate(
                match { result ->
                    result.hasNext() && result.extensions == mapOf("hello" to "world")
                },
            )
            accumulator.getIncrementalPartialResult(false)
        }
    }

    @Test
    fun `hasNext is true if last job launches more jobs`() = runTest {
        val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)
        val subject = NadelIncrementalResultSupport(lazy { accumulator }, channel)
        val firstLock = CompletableDeferred<Boolean>()
        val secondLock = CompletableDeferred<Boolean>()

        every {
            accumulator.accumulate(any())
        } returns Unit
        every {
            accumulator.getIncrementalPartialResult(any())
        } answers {
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(emptyList())
                .hasNext(firstArg())
                .build()
        }

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

        secondLock.complete(true)
        val second = channel.receive()
        assertFalse(second.hasNext())

        assertTrue(channel.toList().isEmpty())

        verifyOrder {
            accumulator.accumulate(
                match { result ->
                    result.extensions == mapOf("id" to 1)
                },
            )
            accumulator.getIncrementalPartialResult(true)

            accumulator.accumulate(
                match { result ->
                    result.extensions == mapOf("id" to 2)
                },
            )
            accumulator.getIncrementalPartialResult(false)
        }
    }

    @Test
    fun `hasNext is true if there is another job still running`() = runTest {
        val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)

        val subject = NadelIncrementalResultSupport(lazy { accumulator }, channel)
        val firstLock = CompletableDeferred<Boolean>()
        val secondLock = CompletableDeferred<Boolean>()

        every {
            accumulator.accumulate(any())
        } returns Unit
        every {
            accumulator.getIncrementalPartialResult(any())
        } answers {
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(emptyList())
                .hasNext(firstArg())
                .build()
        }

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
        assertTrue(firstItem.hasNext())

        secondLock.complete(true)
        val secondItem = channel.receive()
        assertTrue(secondItem !== firstItem)
        assertFalse(secondItem.hasNext())

        verifyOrder {
            accumulator.accumulate(
                match { result ->
                    result.incremental?.isEmpty() == true
                },
            )
            accumulator.getIncrementalPartialResult(true)

            accumulator.accumulate(
                match { result ->
                    (result.incremental?.singleOrNull() as DeferPayload?)?.getData<String>() == "Hello world"
                },
            )
            accumulator.getIncrementalPartialResult(false)
        }
    }

    @Test
    fun `emits nothing if accumulator returns null`() = runTest {
        val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)

        val subject = NadelIncrementalResultSupport(lazy { accumulator }, channel)

        every {
            accumulator.accumulate(any())
        } returns Unit
        every {
            accumulator.getIncrementalPartialResult(any())
        } returns null

        // When
        subject.defer {
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(emptyList())
                .hasNext(false)
                .build()
        }

        subject.defer {
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(emptyList())
                .hasNext(false)
                .build()
        }

        // Then
        subject.onInitialResultComplete()

        val elements = channel.toList()

        assertTrue(elements.size == 1)
        assertFalse(elements[0].hasNext())
        assertTrue(elements[0].incremental!!.isEmpty())

        verifyOrder {
            accumulator.accumulate(any())
            accumulator.getIncrementalPartialResult(true)
            accumulator.accumulate(any())
            accumulator.getIncrementalPartialResult(false)
        }
    }

    @Test
    fun `forwards responses from multiple Flows`() = runTest {
        val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)

        val subject = NadelIncrementalResultSupport(lazy { accumulator }, channel)

        every {
            accumulator.accumulate(any())
        } returns Unit
        every {
            accumulator.getIncrementalPartialResult(any())
        } answers {
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(emptyList())
                .hasNext(firstArg())
                .build()
        }

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

        verifyAll {
            accumulator.accumulate(
                match { result ->
                    result.incremental?.isEmpty() == true && result.extensions == null
                },
            )
            accumulator.getIncrementalPartialResult(true)

            accumulator.accumulate(
                match { result ->
                    result.extensions == mapOf("two" to true)
                },
            )
            accumulator.getIncrementalPartialResult(true)

            accumulator.accumulate(
                match { result ->
                    result.extensions == mapOf("three" to true)
                },
            )
            accumulator.getIncrementalPartialResult(false)
        }
    }

    @Test
    fun `channel completes even if a Flow failed`() {
        var completed = false
        try {
            runTest {
                val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)

                val subject = NadelIncrementalResultSupport(lazy { accumulator }, channel)

                val failureMutex = Mutex(true)

                every {
                    accumulator.accumulate(any())
                } returns Unit
                every {
                    accumulator.getIncrementalPartialResult(any())
                } answers {
                    DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                        .incrementalItems(emptyList())
                        .hasNext(firstArg())
                        .build()
                }

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

                verifyOrder {
                    accumulator.accumulate(any())
                    accumulator.getIncrementalPartialResult(any())
                }

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

        val subject = NadelIncrementalResultSupport(lazy { accumulator }, channel)

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

        val subject = NadelIncrementalResultSupport(lazy { accumulator }, channel)
        val childLock = Mutex(locked = true)

        every {
            accumulator.accumulate(any())
        } returns Unit
        every {
            accumulator.getIncrementalPartialResult(any())
        } answers {
            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(emptyList())
                .hasNext(firstArg())
                .build()
        }

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

        childLock.unlock()

        val child = channel.receive()
        assertFalse(child.hasNext())

        assertTrue(channel.toList().isEmpty())

        verifyOrder {
            accumulator.accumulate(
                match { parent ->
                    parent.extensions == mapOf("parent" to true)
                },
            )
            accumulator.getIncrementalPartialResult(true)

            accumulator.accumulate(
                match { child ->
                    child.extensions == mapOf("child" to true)
                },
            )
            accumulator.getIncrementalPartialResult(false)
        }
    }

    @Test
    fun `errors if multiple elements in Flow are hasNext false`() {
        val exception = assertThrows<IllegalStateException> {
            runTest {
                val channel = Channel<DelayedIncrementalPartialResult>(UNLIMITED)

                val subject = NadelIncrementalResultSupport(lazy { accumulator }, channel)

                every {
                    accumulator.accumulate(any())
                } returns Unit
                every {
                    accumulator.getIncrementalPartialResult(any())
                } answers {
                    DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                        .incrementalItems(emptyList())
                        .hasNext(firstArg())
                        .build()
                }

                val inputResults = listOf(
                    DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                        .incrementalItems(emptyList())
                        .hasNext(false)
                        .build(),
                    DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                        .incrementalItems(emptyList())
                        .hasNext(false)
                        .build(),
                )

                // When
                subject.defer(inputResults.asFlow())

                // Then
                subject.onInitialResultComplete()

                val contents = channel.toList()
                assertTrue(contents.size == 1)
                assertTrue(contents.map { it.hasNext() } == listOf(false))

                verifyOrder {
                    accumulator.accumulate(inputResults[0])
                    accumulator.getIncrementalPartialResult(false)
                    accumulator.accumulate(inputResults[1])
                }
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

                val subject = NadelIncrementalResultSupport(lazy { accumulator }, channel)
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
