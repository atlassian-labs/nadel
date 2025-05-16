package performance

import graphql.nadel.tests.next.fixtures.partition.NestedPartitionTest
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Setup
import java.util.concurrent.TimeUnit

open class NestedPartitionBenchmark: BenchmarkTestBase() {

    @Setup
    override fun setup() {
        test = NestedPartitionTest()
        super.setup()
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    fun bechmarkThroughput() {
        execute()
    }
}