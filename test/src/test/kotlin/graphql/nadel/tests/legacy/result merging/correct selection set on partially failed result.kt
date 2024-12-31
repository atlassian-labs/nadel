package graphql.nadel.tests.legacy.`result merging`

import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.nadel.ServiceExecution
import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import java.util.concurrent.CompletableFuture

public class `correct selection set on partially failed result` : NadelLegacyIntegrationTest(query =
    """
|query {
|  foo
|  bar
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="foo", overallSchema="""
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
, Service(name="bar", overallSchema="""
    |type Query {
    |  bar: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  bar: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("bar") { env ->
          "Hello"}
      }
    }
    )
)){
    override fun makeServiceExecution(service: Service): ServiceExecution {
        if (service.name == "foo") {
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
        } else {
            return super.makeServiceExecution(service)
        }
    }
}
