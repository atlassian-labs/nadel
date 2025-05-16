package performance

import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput
import graphql.nadel.tests.next.NadelIntegrationTest
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup

@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 5)
@Measurement(iterations = 5)
@Fork(2)
open class BenchmarkTestBase {

    lateinit var test: NadelIntegrationTest
    private lateinit var nadel: Nadel
    private lateinit var executionInput: NadelExecutionInput

    open fun setup() {
        nadel = test.makeNadel()
            .build()

        executionInput = test.makeExecutionInput().build()
    }

    fun execute() {
        nadel.execute(executionInput)
    }
}