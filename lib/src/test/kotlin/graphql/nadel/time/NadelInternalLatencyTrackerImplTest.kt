package graphql.nadel.time

import graphql.nadel.test.mock
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import kotlin.test.Test
import kotlin.test.assertTrue

class NadelInternalLatencyTrackerImplTest {
    private val stopwatch = mock<NadelStopwatch>()

    @Test
    fun `stops time as code is running`() {
        val tracker = NadelInternalLatencyTrackerImpl(stopwatch)
        val stopTestCountdownLatch = CountDownLatch(1)
        val stopThreadCountdownLatch = CountDownLatch(1)

        every { stopwatch.start() } returns Unit
        every { stopwatch.stop() } returns Unit

        val thread = Thread {
            tracker.onExternalRun {
                stopTestCountdownLatch.countDown()
                stopThreadCountdownLatch.await()
            }
        }

        // When
        thread.start()

        // Then
        stopTestCountdownLatch.await()
        verify(exactly = 1) {
            stopwatch.stop()
        }
        confirmVerified(stopwatch)

        // When
        stopThreadCountdownLatch.countDown()
        thread.join()

        // Then
        verify(exactly = 1) {
            stopwatch.start()
        }
        confirmVerified(stopwatch)
    }

    @Test
    fun `stops time as supplier is running`() {
        val tracker = NadelInternalLatencyTrackerImpl(stopwatch)
        val stopTestCountdownLatch = CountDownLatch(1)
        val stopThreadCountdownLatch = CountDownLatch(1)

        every { stopwatch.start() } returns Unit
        every { stopwatch.stop() } returns Unit

        var getResult: Any? = null

        val thread = Thread {
            getResult = tracker.onExternalGet {
                stopTestCountdownLatch.countDown()
                stopThreadCountdownLatch.await()
                "Hello world"
            }
        }

        // When
        thread.start()

        // Then
        stopTestCountdownLatch.await()
        verify(exactly = 1) {
            stopwatch.stop()
        }
        confirmVerified(stopwatch)

        // When
        stopThreadCountdownLatch.countDown()
        thread.join()

        // Then
        assertTrue(getResult == "Hello world")
        verify(exactly = 1) {
            stopwatch.start()
        }
        confirmVerified(stopwatch)
    }

    @Test
    fun `stops time as future is running`() {
        val tracker = NadelInternalLatencyTrackerImpl(stopwatch)

        every { stopwatch.start() } returns Unit
        every { stopwatch.stop() } returns Unit

        val rawFuture = CompletableFuture<String>()

        // When
        val wrappedResult = tracker.onExternalFuture(rawFuture)

        // Then
        verify(exactly = 1) {
            stopwatch.stop()
        }
        confirmVerified(stopwatch)
        assertTrue(wrappedResult.getNow("nothing") == "nothing")

        // When
        rawFuture.complete("Hello world")

        // Then
        assertTrue(wrappedResult.getNow("nothing") == "Hello world")
        verify(exactly = 1) {
            stopwatch.start()
        }
        confirmVerified(stopwatch)
    }

    @Test
    fun `stops time even if future fails`() {
        val tracker = NadelInternalLatencyTrackerImpl(stopwatch)

        every { stopwatch.start() } returns Unit
        every { stopwatch.stop() } returns Unit

        val rawFuture = CompletableFuture<String>()

        // When
        val wrappedResult = tracker.onExternalFuture(rawFuture)

        // Then
        verify(exactly = 1) {
            stopwatch.stop()
        }
        confirmVerified(stopwatch)
        assertTrue(wrappedResult.getNow("nothing") == "nothing")

        // When
        rawFuture.completeExceptionally(RuntimeException("Something went wrong"))

        // Then
        assertTrue(wrappedResult.isCompletedExceptionally)
        val exception = assertThrows<ExecutionException> {
            wrappedResult.get()
        }
        assertTrue(exception.cause?.message == "Something went wrong")
        verify(exactly = 1) {
            stopwatch.start()
        }
        confirmVerified(stopwatch)
    }

    @Test
    fun `stops time as supplied future is running`() {
        val tracker = NadelInternalLatencyTrackerImpl(stopwatch)
        val stopSupplierCountdownLatch = CountDownLatch(1)

        every { stopwatch.start() } returns Unit
        every { stopwatch.stop() } returns Unit

        val rawFuture = CompletableFuture<String>()

        // When
        var wrappedFuture: CompletableFuture<String>? = null
        val thread = Thread {
            wrappedFuture = tracker.onExternalFuture {
                stopSupplierCountdownLatch.await()
                rawFuture
            }
        }
        thread.start()

        // Then: stopwatch is started even before supplier completes
        verify(exactly = 1) {
            stopwatch.stop()
        }
        confirmVerified(stopwatch)
        assertTrue(wrappedFuture == null)

        // When: supplier completes
        stopSupplierCountdownLatch.countDown()
        thread.join()

        // Then: we are supplied wrapped future
        assertTrue(wrappedFuture!!.getNow("nothing") == "nothing")

        // When
        rawFuture.complete("Hello world")

        // Then
        assertTrue(wrappedFuture!!.getNow("nothing") == "Hello world")
        verify(exactly = 1) {
            stopwatch.start()
        }
        confirmVerified(stopwatch)
    }
}
