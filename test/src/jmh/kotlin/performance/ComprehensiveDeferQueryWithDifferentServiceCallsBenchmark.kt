package performance

import graphql.nadel.tests.next.fixtures.defer.ComprehensiveDeferQueryWithDifferentServiceCalls
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Setup
import java.util.concurrent.TimeUnit

open class ComprehensiveDeferQueryWithDifferentServiceCallsBenchmark: BenchmarkTestBase() {
    @Setup
    override fun setup() {
        test = ComprehensiveDeferQueryWithDifferentServiceCalls()
        super.setup()
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    fun bechmarkThroughput() {
        execute()
    }
}