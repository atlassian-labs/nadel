package graphql.nadel.tests.legacy.basic

import graphql.language.AstPrinter
import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.nadel.ServiceExecution
import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import java.util.concurrent.CompletableFuture

public class `subscription can be executed` : NadelLegacyIntegrationTest(query = """
|subscription M {
|  onWorldUpdate {
|    id
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name = "MyService",
    overallSchema = """
    |type Query {
    |  hello: World
    |}
    |type World {
    |  id: ID
    |  name: String
    |}
    |type Mutation {
    |  hello: String
    |}
    |type Subscription {
    |  onWorldUpdate: World
    |  onAnotherUpdate: World
    |}
    |""".trimMargin(), underlyingSchema = """
    |type Mutation {
    |  hello: String
    |}
    |
    |type Query {
    |  hello: World
    |}
    |
    |type Subscription {
    |  onAnotherUpdate: World
    |  onWorldUpdate: World
    |}
    |
    |type World {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
        wiring.type("Subscription") { type ->
            type.dataFetcher("onWorldUpdate") { env ->
                null
            }
        }
    }
)
)) {
    override fun makeServiceExecution(service: Service): ServiceExecution {
        return ServiceExecution {
            CompletableFuture.completedFuture(
                NadelServiceExecutionResultImpl(
                    data = mutableMapOf(
                        "onWorldUpdate" to mutableMapOf(
                            "id" to AstPrinter.printAstCompact(it.query),
                        ),
                    ),
                )
            )
        }
    }
}
