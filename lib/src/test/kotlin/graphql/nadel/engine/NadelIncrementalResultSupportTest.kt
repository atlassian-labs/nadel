package graphql.nadel.engine

import graphql.incremental.DeferPayload
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.incremental.DelayedIncrementalPartialResultImpl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        val lockingJob = CompletableDeferred<Boolean>()

        // When
        val firstAsync = subject.defer {
            lockingJob.join()

            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(emptyList())
                .hasNext(true)
                .build()
        }
        val secondAsync = subject.defer {
            lockingJob.join()

            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(emptyList())
                .hasNext(true)
                .build()
        }

        subject.defer {
            // Wait until test tells us to continue
            lockingJob.join()

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

        subject.onInitialResultComplete()

        // Then
        lockingJob.complete(true)
        firstAsync.join()
        secondAsync.join()

        val results = channel.consumeAsFlow().toList()
        assertTrue(results.dropLast(n = 1).all { it.hasNext() })
        val lastResult = results.last()
        assertTrue((lastResult.incremental?.single() as DeferPayload).getData<String>() == "Bye world")
        assertFalse(lastResult.hasNext())
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
                    .hasNext(true)
                    .build()
            }

            DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
                .incrementalItems(emptyList())
                .hasNext(false)
                .build()
        }

        // Then
        firstLock.complete(true)

        val item = channel.receive()
        assertTrue(item.hasNext())
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
