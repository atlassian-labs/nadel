package graphql.nadel.engine.instrumentation

import graphql.nadel.time.NadelParallelElapsedCalculator
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * These tests are weird… because I repurposed them from InternalLatencyCalculatorTest
 */
class NadelParallelElapsedCalculatorTest {
    @Test
    fun `test no calls`() {
        val calculator = NadelParallelElapsedCalculator()

        // Then
        assertTrue(calculator.calculate() == Duration.ZERO)
    }

    @Test
    fun `test one call inside another`() {
        doTest(
            500,
            400,
            "2019-07-11T00:00:00.000Z" to 100,
            "2019-07-11T00:00:00.005Z" to 50,
        )
    }

    @Test
    fun `test two calls at the same time`() {
        doTest(
            500,
            450,
            "2019-07-11T00:00:00.000Z" to 50,
            "2019-07-11T00:00:00.000Z" to 20,
        )
    }

    @Test
    fun `test two calls at the same time with same duration`() {
        doTest(
            500,
            450,
            "2019-07-11T00:00:00.000Z" to 50,
            "2019-07-11T00:00:00.000Z" to 50,
        )
    }

    @Test
    fun `test two calls ending at the same time`() {
        doTest(
            500,
            450,
            "2019-07-11T00:00:00.000Z" to 50,
            "2019-07-11T00:00:00.010Z" to 40,
        )
    }

    @Test
    fun `test two calls ending at the same time in random order`() {
        doTest(
            500,
            450,
            "2019-07-11T00:00:00.010Z" to 40,
            "2019-07-11T00:00:00.000Z" to 50,
        )
    }

    @Test
    fun `test two calls at the same time with overlapping third`() {
        doTest(
            500,
            370,
            "2019-07-11T00:00:00.000Z" to 50,
            "2019-07-11T00:00:00.000Z" to 20,
            "2019-07-11T00:00:00.030Z" to 100,
        )
    }

    @Test
    fun `test two calls at the same time with overlapping third in random order`() {
        doTest(
            500,
            370,
            "2019-07-11T00:00:00.030Z" to 100,
            "2019-07-11T00:00:00.000Z" to 20,
            "2019-07-11T00:00:00.000Z" to 50,
        )
    }

    @Test
    fun `test two call inside another with random order`() {
        doTest(
            500,
            400,
            "2019-07-11T00:00:00.010Z" to 50,
            "2019-07-11T00:00:00.000Z" to 100,
            "2019-07-11T00:00:00.015Z" to 50,
        )
    }

    @Test
    fun `test multiple overlapping calls at the same time`() {
        doTest(
            5000,
            4400,
            "2019-07-11T00:00:00.000Z" to 350,
            "2019-07-11T00:00:00.000Z" to 50,
            "2019-07-11T00:00:00.300Z" to 100,
            "2019-07-11T00:00:00.250Z" to 60,
            "2019-07-11T00:00:00.500Z" to 100,
            "2019-07-11T00:00:00.500Z" to 100,
            "2019-07-11T00:00:00.450Z" to 200,
            "2019-07-11T00:00:00.250Z" to 60,
        )
    }

    @Test
    fun `test multiple overlapping calls`() {
        doTest(
            5000,
            3390,
            "2019-07-11T00:00:10.000Z" to 350,
            "2019-07-11T00:00:00.010Z" to 50,
            "2019-07-11T00:00:00.000Z" to 100,
            "2019-07-11T00:00:00.015Z" to 50,
            "2019-07-11T00:00:00.110Z" to 900,
            "2019-07-11T00:00:00.090Z" to 500,
            "2019-07-11T00:00:02.000Z" to 250,
        )
    }

    @Test
    fun `test non overlapping calls`() {
        doTest(
            500,
            100,
            "2019-07-11T01:00:58Z" to 100,
            "2019-07-11T01:15:58Z" to 300,
        )
    }

    @Test
    fun `test non overlapping calls with wrong order`() {
        doTest(
            500,
            100,
            "2019-07-11T01:15:58Z" to 300,
            "2019-07-11T01:00:58Z" to 100,
        )
    }

    @Test
    fun `test single call`() {
        doTest(
            500,
            400,
            "2019-07-11T01:00:58Z" to 100,
        )
    }

    @Test
    fun `test two overlapping calls`() {
        doTest(
            2500,
            600,
            "2019-07-11T00:00:00Z" to 1500,
            "2019-07-11T00:00:01Z" to 900,
        )
    }

    @Test
    fun `test two overlapping calls with wrong order`() {
        doTest(
            2500,
            1000,
            "2019-07-11T00:00:01Z" to 300,
            "2019-07-11T00:00:00Z" to 1500,
        )
    }

    @Test
    fun `test two perfectly sequential calls`() {
        doTest(
            2500,
            2100,
            "2019-07-11T00:00:01Z" to 300,
            "2019-07-11T00:00:01.300Z" to 100,
        )
    }

    fun doTest(overallLatency: Long, expectedResult: Long, vararg calls: Pair<String, Long>) {
        doTest(overallLatency - expectedResult, *calls)
    }

    fun doTest(expectedResult: Long, vararg calls: Pair<String, Long>) {
        val calculator = NadelParallelElapsedCalculator()

        // Convert (start, duration) to Instant
        val timings: List<Pair<Instant, Instant>> = calls
            .map { (time, duration) ->
                val start = Instant.parse(time)
                val end = start + Duration.ofMillis(duration)
                start to end
            }

        // Finds the min time to figure out where the nanos should be offset from
        val min = timings.minOf { (start) ->
            start
        }

        // Convert to NS
        val timingsNs = timings
            .map { (start, end) ->
                val startNs = start - min
                val endNs = end - min
                startNs to endNs
            }

        timingsNs
            // Timings need to be submitted into calculator as they finish i.e. earliest first
            .sortedBy { (_, end) ->
                end
            }
            .forEach { (start, end) ->
                calculator.submit(start, end)
            }

        val result = calculator.calculate()
        assertTrue(result.toMillis() == expectedResult)
    }
}

operator fun Instant.minus(other: Instant): Duration {
    return Duration.between(other, this)
}
