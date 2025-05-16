package performance

import graphql.nadel.tests.next.fixtures.execution.ServiceExecutionContextTest
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Setup
import java.util.concurrent.TimeUnit

open class ServiceExecutionContextBenchmark: BenchmarkTestBase() {
    @Setup
    override fun setup() {
        test = ServiceExecutionContextTest()
        super.setup()
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    fun bechmarkThroughput() {
        execute()
    }
}