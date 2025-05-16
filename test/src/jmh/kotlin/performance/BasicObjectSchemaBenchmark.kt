package performance

import graphql.nadel.tests.next.fixtures.basic.BasicObjectSchemaTest
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Setup
import java.util.concurrent.TimeUnit

open class BasicObjectSchemaBenchmark: BenchmarkTestBase() {

    @Setup
    override fun setup() {
        test = BasicObjectSchemaTest()
        super.setup()
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    fun bechmarkThroughput() {
        execute()
    }
}