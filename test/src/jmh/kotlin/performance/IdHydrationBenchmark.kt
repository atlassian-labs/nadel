package performance

import graphql.nadel.tests.next.fixtures.hydration.idHydration.IdHydrationTest
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Setup
import java.util.concurrent.TimeUnit

open class IdHydrationBenchmark: BenchmarkTestBase() {
    @Setup
    override fun setup() {
        test = IdHydrationTest()
        super.setup()
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    fun bechmarkThroughput() {
        execute()
    }
}