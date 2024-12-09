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
    private val internalLatency = mock<NadelStopwatch>()

    @Test
    fun `stops time as code is running`() {
        val tracker = NadelInternalLatencyTrackerImpl(internalLatency)
        // Used to tell the test that the external work has started running
        val externalWorkRunning = CountDownLatch(1)
        // Used to stop the external work from completing until the test says so
        val stopExternalWork = CountDownLatch(1)

        every { internalLatency.start() } returns Unit
        every { internalLatency.stop() } returns Unit

        val thread = Thread {
            tracker.onExternalRun {
                externalWorkRunning.countDown()
                stopExternalWork.await()
            }
        }

        // When
        thread.start()

        // Then
        externalWorkRunning.await()
        verify(exactly = 1) {
            internalLatency.stop()
        }
        confirmVerified(internalLatency)

        // When
        stopExternalWork.countDown()
        thread.join()

        // Then
        verify(exactly = 1) {
            internalLatency.start()
        }
        confirmVerified(internalLatency)
    }

    @Test
    fun `stops time as supplier is running`() {
        val tracker = NadelInternalLatencyTrackerImpl(internalLatency)
        // Used to pause the test until the thread runs
        val stopTestCountdownLatch = CountDownLatch(1)
        // Used to stop the thread until the test says so
        val stopThreadCountdownLatch = CountDownLatch(1)

        every { internalLatency.start() } returns Unit
        every { internalLatency.stop() } returns Unit

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
            internalLatency.stop()
        }
        confirmVerified(internalLatency)

        // When
        stopThreadCountdownLatch.countDown()
        thread.join()

        // Then
        assertTrue(getResult == "Hello world")
        verify(exactly = 1) {
            internalLatency.start()
        }
        confirmVerified(internalLatency)
    }

    @Test
    fun `stops time as future is running`() {
        val tracker = NadelInternalLatencyTrackerImpl(internalLatency)

        every { internalLatency.start() } returns Unit
        every { internalLatency.stop() } returns Unit

        val rawFuture = CompletableFuture<String>()

        // When
        val wrappedResult = tracker.onExternalFuture(rawFuture)

        // Then
        verify(exactly = 1) {
            internalLatency.stop()
        }
        confirmVerified(internalLatency)
        assertTrue(wrappedResult.getNow("nothing") == "nothing")

        // When
        rawFuture.complete("Hello world")

        // Then
        assertTrue(wrappedResult.getNow("nothing") == "Hello world")
        verify(exactly = 1) {
            internalLatency.start()
        }
        confirmVerified(internalLatency)
    }

    @Test
    fun `stops time even if future fails`() {
        val tracker = NadelInternalLatencyTrackerImpl(internalLatency)

        every { internalLatency.start() } returns Unit
        every { internalLatency.stop() } returns Unit

        val rawFuture = CompletableFuture<String>()

        // When
        val wrappedResult = tracker.onExternalFuture(rawFuture)

        // Then
        verify(exactly = 1) {
            internalLatency.stop()
        }
        confirmVerified(internalLatency)
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
            internalLatency.start()
        }
        confirmVerified(internalLatency)
    }

    @Test
    fun `stops time as supplied future is running`() {
        val tracker = NadelInternalLatencyTrackerImpl(internalLatency)
        // This is used to pause the supplier until it is counted down
        val stopSupplierCountdownLatch = CountDownLatch(1)
        // This is used to pause the test until the thread runs
        val stopTestCountdownLatch = CountDownLatch(1)

        every { internalLatency.start() } returns Unit
        every { internalLatency.stop() } returns Unit

        val rawFuture = CompletableFuture<String>()

        // When
        var wrappedFuture: CompletableFuture<String>? = null
        val thread = Thread {
            wrappedFuture = tracker.onExternalFuture {
                stopTestCountdownLatch.countDown()
                stopSupplierCountdownLatch.await()
                rawFuture
            }
        }
        thread.start()

        // Then: stopwatch is started even before supplier completes
        stopTestCountdownLatch.await()
        verify(exactly = 1) {
            internalLatency.stop()
        }
        confirmVerified(internalLatency)
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
            internalLatency.start()
        }
        confirmVerified(internalLatency)
    }

    @Test
    fun `keeps internal latency paused if there are still external calls pending`() {
        val tracker = NadelInternalLatencyTrackerImpl(internalLatency)
        // These are counted down when the thread actually starts
        val thread1RunningCountdownLatch = CountDownLatch(1)
        val thread2RunningCountdownLatch = CountDownLatch(1)
        // These are invoked to finish the threads
        val finishThread1CountdownLatch = CountDownLatch(1)
        val finishThread2CountdownLatch = CountDownLatch(1)

        every { internalLatency.start() } returns Unit
        every { internalLatency.stop() } returns Unit

        val thread1 = Thread {
            tracker.onExternalRun {
                thread1RunningCountdownLatch.countDown()
                finishThread1CountdownLatch.await()
            }
        }
        val thread2 = Thread {
            tracker.onExternalRun {
                thread2RunningCountdownLatch.countDown()
                finishThread2CountdownLatch.await()
            }
        }

        // When: start thread ONE external work
        thread1.start()

        // Then: internal latency is stopped
        thread1RunningCountdownLatch.await()
        verify(exactly = 1) {
            internalLatency.stop()
        }
        confirmVerified(internalLatency)

        // When: start thread TWO external work
        thread2.start()

        // Then: does not stop already stopped internal latency
        thread2RunningCountdownLatch.await()
        confirmVerified(internalLatency)

        // When: thread ONE finishes
        finishThread1CountdownLatch.countDown()
        thread1.join()

        // Then: internal latency is not started because there is still pending external work
        confirmVerified(internalLatency)

        // When: thread TWO finishes
        finishThread2CountdownLatch.countDown()
        thread2.join()

        // Then: internal latency is started again as all external work completes
        verify(exactly=1){
            internalLatency.start()
        }
        confirmVerified(internalLatency)
    }
}
