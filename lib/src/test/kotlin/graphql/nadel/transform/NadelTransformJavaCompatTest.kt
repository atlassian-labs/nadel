package graphql.nadel.transform

import graphql.nadel.NadelEngineContext
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransformContext
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformJavaCompat
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.query.NadelQueryTransformerJavaCompat
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.test.NadelTransformJavaCompatAdapter
import graphql.nadel.test.mock
import graphql.nadel.test.spy
import graphql.normalized.ExecutableNormalizedField
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import java.util.concurrent.CompletableFuture

class NadelTransformJavaCompatTest : DescribeSpec({
    describe("create compat") {
        it("delegates name to compat") {
            // given
            val compat = spy(object : NadelTransformJavaCompatAdapter<NadelTransformContext> {
                override val name: String
                    get() = "Test"

                override fun isApplicable(
                    executionContext: NadelExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    services: Map<String, Service>,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): CompletableFuture<NadelTransformContext?> {
                    return CompletableFuture.completedFuture(null)
                }
            })

            val transformer = NadelTransformJavaCompat.create(compat)

            // when
            val name = transformer.name

            // then
            assert(name == "Test")
            verify(exactly = 1) { compat.name }

            confirmVerified(compat)
        }

        it("delegates isApplicable to compat") {
            // given
            class Context : NadelTransformContext {
                val value = 123
            }

            val compat = spy(object : NadelTransformJavaCompatAdapter<Context> {
                override val name: String
                    get() = "Test"

                override fun isApplicable(
                    executionContext: NadelExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    services: Map<String, Service>,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): CompletableFuture<Context?> {
                    return CompletableFuture.completedFuture(Context())
                }
            })

            val transformer = NadelTransformJavaCompat.create(compat)

            val executionContext = mock<NadelExecutionContext>()
            val executionBlueprint = mock<NadelOverallExecutionBlueprint>()
            val services = mock<Map<String, Service>>()
            val service = mock<Service>()
            val overallField = mock<ExecutableNormalizedField>()
            val hydrationDetails = mock<ServiceExecutionHydrationDetails>()
            val engineContext = mock<NadelEngineContext> {
                every { it.services } returns services
                every { it.executionBlueprint } returns executionBlueprint
            }

            // when
            val isApplicable = with(engineContext) {
                with(executionContext) {
                    transformer.isApplicable(
                        service,
                        overallField,
                        hydrationDetails,
                    )
                }
            }

            // then
            assert(isApplicable?.value == 123)

            verify(exactly = 1) {
                compat.isApplicable(
                    executionContext,
                    executionBlueprint,
                    services,
                    service,
                    overallField,
                    hydrationDetails,
                )
            }

            confirmVerified(compat)
        }

        it("delegates transformField to compat") {
            // given
            class Context : NadelTransformContext

            val contextInstance = Context()

            val expectedResult = mock<NadelTransformFieldResult>()
            val compat = spy(object : NadelTransformJavaCompatAdapter<Context> {
                override val name: String
                    get() = "Test"

                override fun isApplicable(
                    executionContext: NadelExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    services: Map<String, Service>,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): CompletableFuture<Context?> {
                    return CompletableFuture.completedFuture(contextInstance)
                }

                override fun transformField(
                    executionContext: NadelExecutionContext,
                    transformer: NadelQueryTransformerJavaCompat,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    field: ExecutableNormalizedField,
                    state: Context,
                ): CompletableFuture<NadelTransformFieldResult> {
                    return CompletableFuture.completedFuture(expectedResult)
                }
            })

            val transformer = NadelTransformJavaCompat.create(compat)

            val executionContext = mock<NadelExecutionContext>()
            val queryTransformer = mock<NadelQueryTransformer>()
            val executionBlueprint = mock<NadelOverallExecutionBlueprint>()
            val field = mock<ExecutableNormalizedField>()
            val engineState = mock<NadelEngineContext> {
                every { it.executionBlueprint } returns executionBlueprint
            }

            // when
            val transformField = with(engineState) {
                with(executionContext) {
                    with(contextInstance) {
                        transformer.transformField(
                            queryTransformer,
                            field,
                        )
                    }
                }
            }

            // then
            assert(transformField === expectedResult)

            verify(exactly = 1) {
                compat.transformField(
                    executionContext,
                    // This is another wrapper for Java compat
                    match { it.queryTransformer == queryTransformer },
                    executionBlueprint,
                    field,
                    contextInstance,
                )
            }

            confirmVerified(compat)
        }

        it("delegates getResultInstructions to compat") {
            // given
            val expectedResult = listOf<NadelResultInstruction>(
                NadelResultInstruction.Set(
                    subject = JsonNode(mutableMapOf<String, Any?>()),
                    key = NadelResultKey("hello"),
                    newValue = JsonNode(1),
                )
            )

            class TestContext : NadelTransformContext

            val testContextInstance = TestContext()

            val compat = spy(object : NadelTransformJavaCompatAdapter<TestContext> {
                override val name: String
                    get() = "Test"

                override fun isApplicable(
                    executionContext: NadelExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    services: Map<String, Service>,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): CompletableFuture<TestContext?> {
                    return CompletableFuture.completedFuture(testContextInstance)
                }

                override fun getResultInstructions(
                    executionContext: NadelExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    overallField: ExecutableNormalizedField,
                    underlyingParentField: ExecutableNormalizedField?,
                    result: ServiceExecutionResult,
                    state: TestContext,
                    nodes: JsonNodes,
                ): CompletableFuture<List<NadelResultInstruction>> {
                    return CompletableFuture.completedFuture(expectedResult)
                }
            })

            val transformer = NadelTransformJavaCompat.create(compat)

            val executionContext = mock<NadelExecutionContext>()
            val executionBlueprint = mock<NadelOverallExecutionBlueprint>()
            val overallField = mock<ExecutableNormalizedField>()
            val underlyingParentField = mock<ExecutableNormalizedField>()
            val result = mock<ServiceExecutionResult>()
            val nodes = mock<JsonNodes>()
            val engineContext = mock<NadelEngineContext> {
                every { it.executionBlueprint } returns executionBlueprint
            }

            // when
            val getResultInstructions = with(engineContext) {
                with(executionContext) {
                    with(testContextInstance) {
                        transformer.getResultInstructions(
                            overallField,
                            underlyingParentField,
                            result,
                            nodes,
                        )
                    }
                }
            }

            // then
            assert(getResultInstructions === expectedResult)

            verify(exactly = 1) {
                compat.getResultInstructions(
                    executionContext,
                    executionBlueprint,
                    overallField,
                    underlyingParentField,
                    result,
                    testContextInstance,
                    nodes,
                )
            }

            confirmVerified(compat)
        }
    }
})
