package graphql.nadel.tests.next.fixtures.execution

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.Nadel
import graphql.nadel.ServiceExecution
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldContext
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformOperationContext
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.hooks.NadelCreateOperationExecutionContextParams
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.normalized.ExecutableNormalizedField
import java.util.Collections
import java.util.concurrent.CompletableFuture
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServiceExecutionContextTest : NadelIntegrationTest(
    query = """
        query {
          bug: issue(id: 6) {
            title
          }
          me {
            lastWorkedOn {
              title
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "monolith",
            overallSchema = """
                type Query {
                  me: User
                  issue(id: ID!): Issue
                }
                type User {
                  id: ID!
                  lastWorkedOnId: ID! @hidden
                  lastWorkedOn: Issue
                    @hydrated(
                      service: "monolith"
                      field: "issue"
                      arguments: [
                        {name: "id", value: "$source.lastWorkedOnId"}
                      ]
                    )
                }
                type Issue {
                  id: ID!
                  title: String!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class User(
                    val id: String,
                    val lastWorkedOnId: String,
                )

                data class Issue(
                    val id: String,
                    val title: String,
                )

                val issuesById = listOf(
                    Issue(id = "6", title = "Fix cloud ID header not set"),
                    Issue(id = "9", title = "Improve cloud ID extraction"),
                ).strictAssociateBy { it.id }

                val me = User(
                    id = "1",
                    lastWorkedOnId = "9",
                )

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("me") { env ->
                                me
                            }
                            .dataFetcher("issue") { env ->
                                issuesById[env.getArgument<String>("id")]
                            }
                    }
            },
        )
    ),
) {
    private val serviceExecutionContexts = Collections.synchronizedList(mutableListOf<TestOperationExecutionContext>())
    private val transformServiceExecutionContexts =
        Collections.synchronizedList(mutableListOf<TestTransformOperationContext>())

    private class TestOperationExecutionContext(
        override val parentContext: NadelExecutionContext,
        override val service: graphql.nadel.Service,
        override val topLevelField: ExecutableNormalizedField,
    ) : NadelOperationExecutionContext() {
        val getTransformFieldContext = Collections.synchronizedList(mutableListOf<String>())
        val transformField = Collections.synchronizedList(mutableListOf<String>())
        val transformResult = Collections.synchronizedList(mutableListOf<String>())

        override fun toString(): String {
            return "ServiceExecutionContext(getTransformFieldContext=$getTransformFieldContext, transformField=$transformField, transformResult=$transformResult)"
        }
    }

    private data class TestTransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
        val rootField: String,
        var onCompleteRan: Boolean = false,
    ) : NadelTransformOperationContext() {
        val getTransformFieldContext = Collections.synchronizedList(mutableListOf<String>())
        val transformField = Collections.synchronizedList(mutableListOf<String>())
        val transformResult = Collections.synchronizedList(mutableListOf<String>())
    }

    fun ExecutableNormalizedField.toExecutionString(): String {
        val objects = objectTypeNames.joinToString(separator = ",", prefix = "[", postfix = "]")
        val args = normalizedArguments.entries.joinToString(
            separator = ",",
            prefix = "(",
            postfix = ")",
        ) { (argName, argValue) ->
            "$argName = $argValue"
        }
        var str = "$objects.$fieldName$args"
        if (alias != null) {
            str = "$alias: $str"
        }
        return str
    }

    private data class TransformFieldContext(
        override val parentContext: TestTransformOperationContext,
        override val overallField: ExecutableNormalizedField,
    ) : NadelTransformFieldContext<TestTransformOperationContext>()

    override fun makeNadel(): Nadel.Builder {
        return super.makeNadel()
            .executionHooks(
                object : NadelExecutionHooks {
                    override fun createOperationExecutionContext(
                        params: NadelCreateOperationExecutionContextParams,
                    ): CompletableFuture<NadelOperationExecutionContext> {
                        return CompletableFuture.completedFuture(
                            TestOperationExecutionContext(
                                parentContext = params.executionContext,
                                service = params.service,
                                topLevelField = params.topLevelField,
                            )
                        )
                    }
                },
            )
            .transforms(
                listOf(
                    object : NadelTransform<TestTransformOperationContext, TransformFieldContext> {
                        override suspend fun getTransformOperationContext(
                            operationExecutionContext: NadelOperationExecutionContext,
                        ): TestTransformOperationContext {
                            val testTransformServiceExecutionContext = TestTransformOperationContext(
                                parentContext = operationExecutionContext,
                                rootField = operationExecutionContext.topLevelField.toExecutionString(),
                            )
                            transformServiceExecutionContexts.add(testTransformServiceExecutionContext)
                            return testTransformServiceExecutionContext
                        }

                        override suspend fun getTransformFieldContext(
                            transformContext: TestTransformOperationContext,
                            overallField: ExecutableNormalizedField,
                        ): TransformFieldContext? {
                            val f = overallField.toExecutionString()
                            transformContext.getTransformFieldContext.add(f)
                            (transformContext.operationExecutionContext as TestOperationExecutionContext)
                                .getTransformFieldContext.add(f)
                            return TransformFieldContext(transformContext, overallField)
                        }

                        override suspend fun transformField(
                            transformContext: TransformFieldContext,
                            transformer: NadelQueryTransformer,
                            field: ExecutableNormalizedField,
                        ): NadelTransformFieldResult {
                            val f = field.toExecutionString()
                            transformContext.transformOperationContext.transformField.add(f)
                            (transformContext.operationExecutionContext as TestOperationExecutionContext)
                                .transformField.add(f)
                            return NadelTransformFieldResult.unmodified(field)
                        }

                        override suspend fun transformResult(
                            transformContext: TransformFieldContext,
                            underlyingParentField: ExecutableNormalizedField?,
                            resultNodes: JsonNodes,
                        ): List<NadelResultInstruction> {
                            val f = transformContext.overallField.toExecutionString()
                            transformContext.transformOperationContext.transformResult.add(f)
                            (transformContext.operationExecutionContext as TestOperationExecutionContext)
                                .transformResult.add(f)
                            return emptyList()
                        }

                        override suspend fun onComplete(
                            transformContext: TestTransformOperationContext,
                            resultNodes: JsonNodes,
                        ) {
                            val context = transformContext
                            assertFalse(context.onCompleteRan, "on complete is supposed to run only once")
                            context.onCompleteRan = true
                        }
                    }
                )
            )
    }

    override fun makeServiceExecution(service: Service): ServiceExecution {
        val impl = super.makeServiceExecution(service)

        return ServiceExecution {
            serviceExecutionContexts.add(it.serviceExecutionContext as TestOperationExecutionContext)
            impl.execute(it)
        }
    }

    override fun assert(result: ExecutionResult, incrementalResults: List<DelayedIncrementalPartialResult>?) {
        assertServiceExecutionContexts()
        assertTransformServiceExecutionContexts()
    }

    private fun assertServiceExecutionContexts() {
        // Three separate executions, two for top level fields, and one for hydration
        assertTrue(serviceExecutionContexts.size == 3)

        val me = serviceExecutionContexts.single {
            it.getTransformFieldContext.first().contains("me()")
        }
        val expectedMeExecutions = listOf(
            "[Query].me()",
            "[User].lastWorkedOn()",
            "[Issue].title()",
        )
        assertTrue(me.getTransformFieldContext == expectedMeExecutions)
        assertTrue(me.transformField == expectedMeExecutions.dropLast(1)) // dropLast as child is removed due to hydration
        assertTrue(me.transformResult.sorted() == expectedMeExecutions.dropLast(1).sorted())

        val bug = serviceExecutionContexts.single {
            it.getTransformFieldContext.first().contains("bug")
        }
        val expectedBugExecutions = listOf(
            "bug: [Query].issue(id = NormalizedInputValue{typeName='ID!', value=IntValue{value=6}})",
            "[Issue].title()",
        )
        assertTrue(bug.getTransformFieldContext == expectedBugExecutions)
        assertTrue(bug.transformField == expectedBugExecutions)
        assertTrue(bug.transformResult.sorted() == expectedBugExecutions.sorted())

        val hydration = serviceExecutionContexts.single {
            it.getTransformFieldContext.first().contains("9")
        }
        val expectedHydrationExecutions = listOf(
            "[Query].issue(id = NormalizedInputValue{typeName='ID!', value=StringValue{value='9'}})",
            "[Issue].title()",
        )
        assertTrue(hydration.getTransformFieldContext == expectedHydrationExecutions)
        assertTrue(hydration.transformField == expectedHydrationExecutions)
        assertTrue(hydration.transformResult.sorted() == expectedHydrationExecutions.sorted())
    }

    private fun assertTransformServiceExecutionContexts() {
        // Three separate executions, two for top level fields, and one for hydration
        assertTrue(transformServiceExecutionContexts.size == 3)

        val me = transformServiceExecutionContexts.single {
            it.getTransformFieldContext.first().contains("me()")
        }
        val expectedMeExecutions = listOf(
            "[Query].me()",
            "[User].lastWorkedOn()",
            "[Issue].title()",
        )
        assertTrue(me.getTransformFieldContext == expectedMeExecutions)
        assertTrue(me.transformField == expectedMeExecutions.dropLast(1)) // dropLast as child is removed due to hydration
        assertTrue(me.transformResult.sorted() == expectedMeExecutions.dropLast(1).sorted())
        assertTrue(me.rootField == "[Query].me()")

        val bug = transformServiceExecutionContexts.single {
            it.getTransformFieldContext.first().contains("bug")
        }
        val expectedBugExecutions = listOf(
            "bug: [Query].issue(id = NormalizedInputValue{typeName='ID!', value=IntValue{value=6}})",
            "[Issue].title()",
        )
        assertTrue(bug.getTransformFieldContext == expectedBugExecutions)
        assertTrue(bug.transformField == expectedBugExecutions)
        assertTrue(bug.transformResult.sorted() == expectedBugExecutions.sorted())
        assertTrue(bug.rootField == "bug: [Query].issue(id = NormalizedInputValue{typeName='ID!', value=IntValue{value=6}})")

        val hydration = transformServiceExecutionContexts.single {
            it.getTransformFieldContext.first().contains("9")
        }
        val expectedHydrationExecutions = listOf(
            "[Query].issue(id = NormalizedInputValue{typeName='ID!', value=StringValue{value='9'}})",
            "[Issue].title()",
        )
        assertTrue(hydration.getTransformFieldContext == expectedHydrationExecutions)
        assertTrue(hydration.transformField == expectedHydrationExecutions)
        assertTrue(hydration.transformResult.sorted() == expectedHydrationExecutions.sorted())
        assertTrue(hydration.rootField == "[Query].issue(id = NormalizedInputValue{typeName='ID!', value=StringValue{value='9'}})")

        assertTrue(transformServiceExecutionContexts.all { it.onCompleteRan })
    }
}
