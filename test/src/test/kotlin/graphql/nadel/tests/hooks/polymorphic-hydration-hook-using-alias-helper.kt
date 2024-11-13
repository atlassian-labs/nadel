package graphql.nadel.tests.hooks

import graphql.Assert.assertTrue
import graphql.ExecutionResult
import graphql.nadel.Nadel
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.util.serviceExecutionFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import java.util.Collections
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private class PolymorphicHydrationHookUsingAliasHelper : NadelExecutionHooks {
    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
        instructions: List<T>,
        parentNode: JsonNode,
        aliasHelper: NadelAliasHelper,
        userContext: Any?,
    ): T? {
        return instructions.firstOrNull {
            val argument = it.backingFieldArguments.single()
            if (argument is NadelHydrationArgument.SourceField) {
                val backingFieldName = argument.sourceFieldDef.name
                val targetFieldName = aliasHelper.getResultKey(backingFieldName)
                val hydrationArgumentValue = (parentNode.value as JsonMap)[targetFieldName]
                hydrationInstructionMatchesArgumentValue(it, hydrationArgumentValue as String)
            } else {
                false
            }
        }
    }

    private fun <T : NadelGenericHydrationInstruction> hydrationInstructionMatchesArgumentValue(
        instruction: T,
        hydrationArgumentValue: String,
    ): Boolean {
        return (instruction.backingFieldDef.name.contains("pet")
            && hydrationArgumentValue.startsWith("pet", ignoreCase = true))
            || (instruction.backingFieldDef.name.contains("human")
            && hydrationArgumentValue.startsWith("human", ignoreCase = true))
    }
}

open class PolymorphicHydrationWithAliasTestHook : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.executionHooks(PolymorphicHydrationHookUsingAliasHelper())
    }
}

@UseHook
class `batch-polymorphic-hydration` : PolymorphicHydrationWithAliasTestHook()

@UseHook
class `batch-polymorphic-hydration-actor-fields-are-in-the-same-service` : PolymorphicHydrationWithAliasTestHook()

@UseHook
class `batch-polymorphic-hydration-actor-fields-are-in-the-same-service-return-types-implement-same-interface` :
    PolymorphicHydrationWithAliasTestHook()

@UseHook
class `batch-polymorphic-hydrations-are-executed-in-parallel` : PolymorphicHydrationWithAliasTestHook() {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        val defaultServiceExecutionFactory = builder.serviceExecutionFactory
        val servicesExecuted = Collections.synchronizedSet(HashSet<String>())

        return super.makeNadel(builder)
            .serviceExecutionFactory(
                object : ServiceExecutionFactory {
                    override fun getServiceExecution(serviceName: String): ServiceExecution {
                        val defaultServiceExecution = defaultServiceExecutionFactory.getServiceExecution(serviceName)

                        return ServiceExecution { params ->
                            coroutineScope
                                .future {
                                    servicesExecuted.add(serviceName)
                                    if (params.isHydrationCall) {
                                        delay(Random.nextInt(400, 800).milliseconds)
                                        assertTrue(servicesExecuted == hashSetOf("people", "pets", "foo"))
                                    }
                                }
                                .thenCompose {
                                    defaultServiceExecution.execute(params)
                                }
                        }
                    }
                }
            )
    }

    override fun assertResult(result: ExecutionResult) {
        super.assertResult(result)
        coroutineScope.cancel()
    }

    override fun assertFailure(throwable: Throwable): Boolean {
        coroutineScope.cancel()
        return super.assertFailure(throwable)
    }
}
