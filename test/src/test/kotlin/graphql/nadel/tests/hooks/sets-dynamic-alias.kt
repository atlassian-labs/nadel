package graphql.nadel.tests.hooks

import graphql.language.AstPrinter
import graphql.nadel.Nadel
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.transform.artificial.AliasGenerator
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.KeepHook
import graphql.nadel.tests.NadelEngineType
import graphql.nadel.tests.util.serviceExecutionFactory
import java.util.concurrent.CompletableFuture

@KeepHook
class `sets-dynamic-alias` : EngineTestHook {
    private fun Regex.findOrDie(input: CharSequence, what: String): MatchResult {
        return find(input) ?: error("Unable to match $what")
    }

    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        val defaultServiceExecutionFactory = builder.serviceExecutionFactory
        // Enables dynamic aliases with UUID suffixes
        AliasGenerator.isStatic = false

        return builder.serviceExecutionFactory(object : ServiceExecutionFactory by defaultServiceExecutionFactory {
            override fun getServiceExecution(serviceName: String): ServiceExecution {
                return ServiceExecution { params ->
                    val printedQuery = AstPrinter.printAst(params.query)
                    println(printedQuery)

                    val barAlias = Regex("__([a-zA-Z0-9]+?)__bar")
                        .findOrDie(printedQuery, what = "bar alias")
                        .groupValues[1]
                    val phoneAlias = Regex("__([a-zA-Z0-9]+?)__landline")
                        .findOrDie(printedQuery, what = "phone alias")
                        .groupValues[1]

                    CompletableFuture.completedFuture(
                        ServiceExecutionResult(
                            mutableMapOf(
                                "rename__foo__${barAlias}__bar" to mutableMapOf(
                                    "deep_rename__phoneNumber__${phoneAlias}__landline" to mutableMapOf(
                                        "number" to "aoeu"
                                    ),
                                    "__typename__deep_rename__phoneNumber__$phoneAlias" to "Bar",
                                ),
                            ).also(::println) as Map<String, Any>,
                        ),
                    )
                }
            }
        })
    }
}
