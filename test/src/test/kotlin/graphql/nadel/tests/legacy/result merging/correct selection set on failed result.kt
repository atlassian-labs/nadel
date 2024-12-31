package graphql.nadel.tests.legacy.`result merging`

import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.nadel.ServiceExecution
import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import java.util.concurrent.CompletableFuture

public class `correct selection set on failed result` : NadelLegacyIntegrationTest(query = """
|query {
|  foo
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  foo: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foo: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
    }
    )
)){
    override fun makeServiceExecution(service: Service): ServiceExecution {
        return ServiceExecution {
            CompletableFuture.completedFuture(
                NadelServiceExecutionResultImpl(
                    data = mutableMapOf(),
                    errors = mutableListOf(
                        mutableMapOf(
                            "message" to "Test",
                        ),
                    ),
                ),
            )
        }
    }
}
