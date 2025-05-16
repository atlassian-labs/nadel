package performance

import graphql.nadel.tests.next.fixtures.basic.BasicObjectSchemaTest
import graphql.nadel.tests.next.fixtures.basic.EchoTest
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Setup
import java.util.concurrent.TimeUnit

open class EchoBenchmark: BenchmarkTestBase() {
    @Setup
    override fun setup() {
        test = EchoTest()
        super.setup()
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    fun bechmarkThroughput() {
        execute()
    }
}