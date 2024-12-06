package graphql.nadel

import graphql.nadel.time.NadelStopwatch
import kotlin.test.Test
import kotlin.test.assertTrue

class NadelStopwatchTest {
    @Test
    fun `returns zero if stopwatch is never started`() {
        var time = 0L
        val ticker: () -> Long = { time }
        val stopwatch = NadelStopwatch(ticker)

        // When
        assertTrue(stopwatch.elapsed().toNanos() == 0L)
        time = 100
        assertTrue(stopwatch.elapsed().toNanos() == 0L)
    }

    @Test
    fun `returns elapsed time if stopwatch is still running`() {
        var time = 0L
        val ticker: () -> Long = { time }
        val stopwatch = NadelStopwatch(ticker)

        // When
        stopwatch.start()
        assertTrue(stopwatch.elapsed().toNanos() == 0L)
        time = 100
        assertTrue(stopwatch.elapsed().toNanos() == 100L)
    }

    @Test
    fun `stopwatch elapsed is frozen when stopped`() {
        var time = 0L
        val ticker: () -> Long = { time }
        val stopwatch = NadelStopwatch(ticker)

        // When
        stopwatch.start()
        assertTrue(stopwatch.elapsed().toNanos() == 0L)
        time = 100
        assertTrue(stopwatch.elapsed().toNanos() == 100L)
        stopwatch.stop()
        time = 300L
        assertTrue(stopwatch.elapsed().toNanos() == 100L)
    }

    @Test
    fun `stopwatch can be resumed`() {
        var time = 0L
        val ticker: () -> Long = { time }
        val stopwatch = NadelStopwatch(ticker)

        // When
        stopwatch.start()
        assertTrue(stopwatch.elapsed().toNanos() == 0L)
        time = 100

        assertTrue(stopwatch.elapsed().toNanos() == 100L)
        stopwatch.stop()

        time = 300L
        stopwatch.start()
        time = 305L
        assertTrue(stopwatch.elapsed().toNanos() == 105L)
    }

    @Test
    fun `invoking start running stopwatch does nothing`() {
        var time = 0L
        val ticker: () -> Long = { time }
        val stopwatch = NadelStopwatch(ticker)

        // When
        repeat(4) {
            stopwatch.start()
        }
        assertTrue(stopwatch.elapsed().toNanos() == 0L)
        time = 100

        repeat(4) {
            stopwatch.start()
        }
        assertTrue(stopwatch.elapsed().toNanos() == 100L)
    }

    @Test
    fun `invoking stop on stopped stopwatch does nothing`() {
        var time = 0L
        val ticker: () -> Long = { time }
        val stopwatch = NadelStopwatch(ticker)

        // When
        stopwatch.start()
        assertTrue(stopwatch.elapsed().toNanos() == 0L)
        stopwatch.stop()
        assertTrue(stopwatch.elapsed().toNanos() == 0L)
        repeat(4) {
            stopwatch.stop()
        }
        assertTrue(stopwatch.elapsed().toNanos() == 0L)

        time = 100L
        stopwatch.start()
        time = 110L
        assertTrue(stopwatch.elapsed().toNanos() == 10L)
        repeat(3) {
            stopwatch.stop()
        }
        assertTrue(stopwatch.elapsed().toNanos() == 10L)
    }
}
