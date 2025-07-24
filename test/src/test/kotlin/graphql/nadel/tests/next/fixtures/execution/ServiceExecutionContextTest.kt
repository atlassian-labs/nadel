package graphql.nadel.tests.next.fixtures.execution

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.Nadel
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.hooks.NadelCreateServiceExecutionContextParams
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.normalized.ExecutableNormalizedField
import java.util.Collections
import java.util.concurrent.CompletableFuture
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
    private val serviceExecutionContexts = Collections.synchronizedList(mutableListOf<TestServiceExecutionContext>())

    private class TestServiceExecutionContext : NadelServiceExecutionContext() {
        val isApplicable = Collections.synchronizedList(mutableListOf<String>())
        val transformField = Collections.synchronizedList(mutableListOf<String>())
        val getResultInstructions = Collections.synchronizedList(mutableListOf<String>())
        val getFinaliseStates = Collections.synchronizedList(mutableListOf<String>())

        override fun toString(): String {
            return "ServiceExecutionContext(isApplicable=$isApplicable, transformField=$transformField, getResultInstructions=$getResultInstructions)"
        }
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

    override fun makeNadel(): Nadel.Builder {
        return super.makeNadel()
            .executionHooks(
                object : NadelExecutionHooks {
                    override fun createServiceExecutionContext(
                        params: NadelCreateServiceExecutionContextParams,
                    ): CompletableFuture<NadelServiceExecutionContext> {
                        return CompletableFuture.completedFuture(TestServiceExecutionContext())
                    }
                },
            )
            .transforms(
                listOf(
                    object : NadelTransform<ExecutableNormalizedField> {
                        override suspend fun isApplicable(
                            executionContext: NadelExecutionContext,
                            serviceExecutionContext: NadelServiceExecutionContext,
                            executionBlueprint: NadelOverallExecutionBlueprint,
                            services: Map<String, graphql.nadel.Service>,
                            service: graphql.nadel.Service,
                            overallField: ExecutableNormalizedField,
                            hydrationDetails: ServiceExecutionHydrationDetails?,
                        ): ExecutableNormalizedField? {
                            (serviceExecutionContext as TestServiceExecutionContext).isApplicable.add(overallField.toExecutionString())
                            return overallField
                        }

                        override suspend fun transformField(
                            executionContext: NadelExecutionContext,
                            serviceExecutionContext: NadelServiceExecutionContext,
                            transformer: NadelQueryTransformer,
                            executionBlueprint: NadelOverallExecutionBlueprint,
                            service: graphql.nadel.Service,
                            field: ExecutableNormalizedField,
                            state: ExecutableNormalizedField,
                        ): NadelTransformFieldResult {
                            (serviceExecutionContext as TestServiceExecutionContext).transformField.add(field.toExecutionString())
                            return NadelTransformFieldResult.unmodified(field)
                        }

                        override suspend fun getResultInstructions(
                            executionContext: NadelExecutionContext,
                            serviceExecutionContext: NadelServiceExecutionContext,
                            executionBlueprint: NadelOverallExecutionBlueprint,
                            service: graphql.nadel.Service,
                            overallField: ExecutableNormalizedField,
                            underlyingParentField: ExecutableNormalizedField?,
                            result: ServiceExecutionResult,
                            state: ExecutableNormalizedField,
                            nodes: JsonNodes,
                        ): List<NadelResultInstruction> {
                            (serviceExecutionContext as TestServiceExecutionContext).getResultInstructions.add(
                                overallField.toExecutionString()
                            )
                            return emptyList()
                        }

                        override suspend fun finalize(
                            executionContext: NadelExecutionContext,
                            serviceExecutionContext: NadelServiceExecutionContext,
                            executionBlueprint: NadelOverallExecutionBlueprint,
                            service: graphql.nadel.Service,
                            result: ServiceExecutionResult,
                            states: List<ExecutableNormalizedField>,
                            nodes: JsonNodes,
                        ) {
                            (serviceExecutionContext as TestServiceExecutionContext).getFinaliseStates.addAll(
                                states.map { it.toExecutionString() }
                            )
                        }
                    }
                )
            )
    }

    override fun makeServiceExecution(service: Service): ServiceExecution {
        val impl = super.makeServiceExecution(service)

        return ServiceExecution {
            serviceExecutionContexts.add(it.serviceExecutionContext as TestServiceExecutionContext)
            impl.execute(it)
        }
    }

    override fun assert(result: ExecutionResult, incrementalResults: List<DelayedIncrementalPartialResult>?) {
        // Three separate executions, two for top level fields, and one for hydration
        assertTrue(serviceExecutionContexts.size == 3)

        val me = serviceExecutionContexts.single {
            it.isApplicable.first().contains("me()")
        }
        val expectedMeExecutions = listOf(
            "[Query].me()",
            "[User].lastWorkedOn()",
            "[Issue].title()",
        )
        assertTrue(me.isApplicable == expectedMeExecutions)
        assertTrue(me.transformField == expectedMeExecutions.dropLast(1)) // dropLast as child is removed due to hydration
        assertTrue(me.getResultInstructions.sorted() == expectedMeExecutions.dropLast(1).sorted())
        assertTrue(me.getFinaliseStates == expectedMeExecutions.dropLast(1).sorted())

        val bug = serviceExecutionContexts.single {
            it.isApplicable.first().contains("bug")
        }
        val expectedBugExecutions = listOf(
            "bug: [Query].issue(id = NormalizedInputValue{typeName='ID!', value=IntValue{value=6}})",
            "[Issue].title()",
        )
        assertTrue(bug.isApplicable == expectedBugExecutions)
        assertTrue(bug.transformField == expectedBugExecutions)
        assertTrue(bug.getResultInstructions.sorted() == expectedBugExecutions.sorted())
        assertTrue(bug.getFinaliseStates.sorted() == expectedBugExecutions.sorted())

        val hydration = serviceExecutionContexts.single {
            it.isApplicable.first().contains("9")
        }
        val expectedHydrationExecutions = listOf(
            "[Query].issue(id = NormalizedInputValue{typeName='ID!', value=StringValue{value='9'}})",
            "[Issue].title()",
        )
        assertTrue(hydration.isApplicable == expectedHydrationExecutions)
        assertTrue(hydration.transformField == expectedHydrationExecutions)
        assertTrue(hydration.getResultInstructions.sorted() == expectedHydrationExecutions.sorted())
        assertTrue(hydration.getFinaliseStates.sorted() == expectedHydrationExecutions.sorted())
    }
}
