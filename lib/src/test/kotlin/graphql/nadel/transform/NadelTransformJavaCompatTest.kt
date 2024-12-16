package graphql.nadel.transform

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.ServiceLike
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
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
import io.mockk.verify
import java.util.concurrent.CompletableFuture

class NadelTransformJavaCompatTest : DescribeSpec({
    describe("create compat") {
        it("delegates name to compat") {
            // given
            val compat = spy(object : NadelTransformJavaCompatAdapter {
                override val name: String
                    get() = "Test"
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
            val compat = spy(object : NadelTransformJavaCompatAdapter {
                override val name: String
                    get() = "Test"

                override fun isApplicable(
                    executionContext: NadelExecutionContext,
                    serviceExecutionContext: NadelServiceExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    services: Map<String, Service>,
                    service: ServiceLike,
                    overallField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): CompletableFuture<Any?> {
                    return CompletableFuture.completedFuture(123)
                }
            })

            val transformer = NadelTransformJavaCompat.create(compat)

            val executionContext = mock<NadelExecutionContext>()
            val serviceExecutionContext = mock<NadelServiceExecutionContext>()
            val executionBlueprint = mock<NadelOverallExecutionBlueprint>()
            val services = mock<Map<String, Service>>()
            val service = mock<Service>()
            val overallField = mock<ExecutableNormalizedField>()
            val hydrationDetails = mock<ServiceExecutionHydrationDetails>()

            // when
            val isApplicable = transformer.isApplicable(
                executionContext,
                serviceExecutionContext,
                executionBlueprint,
                services,
                service,
                overallField,
                hydrationDetails,
            )

            // then
            assert(isApplicable == 123)

            verify(exactly = 1) {
                compat.isApplicable(
                    executionContext,
                    serviceExecutionContext,
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
            val expectedResult = mock<NadelTransformFieldResult>()
            val compat = spy(object : NadelTransformJavaCompatAdapter {
                override val name: String
                    get() = "Test"

                override fun transformField(
                    executionContext: NadelExecutionContext,
                    serviceExecutionContext: NadelServiceExecutionContext,
                    transformer: NadelQueryTransformerJavaCompat,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    service: ServiceLike,
                    field: ExecutableNormalizedField,
                    state: Any,
                ): CompletableFuture<NadelTransformFieldResult> {
                    return CompletableFuture.completedFuture(expectedResult)
                }
            })

            val transformer = NadelTransformJavaCompat.create(compat)

            val executionContext = mock<NadelExecutionContext>()
            val serviceExecutionContext = mock<NadelServiceExecutionContext>()
            val queryTransformer = mock<NadelQueryTransformer>()
            val executionBlueprint = mock<NadelOverallExecutionBlueprint>()
            val service = mock<Service>()
            val field = mock<ExecutableNormalizedField>()
            val state = mock<Any>()

            // when
            val transformField = transformer.transformField(
                executionContext,
                serviceExecutionContext,
                queryTransformer,
                executionBlueprint,
                service,
                field,
                state,
            )

            // then
            assert(transformField === expectedResult)

            verify(exactly = 1) {
                compat.transformField(
                    executionContext,
                    serviceExecutionContext,
                    // This is another wrapper for Java compat
                    match { it.queryTransformer == queryTransformer },
                    executionBlueprint,
                    service,
                    field,
                    state,
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
            val compat = spy(object : NadelTransformJavaCompatAdapter {
                override val name: String
                    get() = "Test"

                override fun getResultInstructions(
                    executionContext: NadelExecutionContext,
                    serviceExecutionContext: NadelServiceExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    service: ServiceLike,
                    overallField: ExecutableNormalizedField,
                    underlyingParentField: ExecutableNormalizedField?,
                    result: ServiceExecutionResult,
                    state: Any,
                    nodes: JsonNodes,
                ): CompletableFuture<List<NadelResultInstruction>> {
                    return CompletableFuture.completedFuture(expectedResult)
                }
            })

            val transformer = NadelTransformJavaCompat.create(compat)

            val executionContext = mock<NadelExecutionContext>()
            val serviceExecutionContext = mock<NadelServiceExecutionContext>()
            val executionBlueprint = mock<NadelOverallExecutionBlueprint>()
            val service = mock<Service>()
            val overallField = mock<ExecutableNormalizedField>()
            val underlyingParentField = mock<ExecutableNormalizedField>()
            val result = mock<ServiceExecutionResult>()
            val state = mock<Any>()
            val nodes = mock<JsonNodes>()

            // when
            val getResultInstructions = transformer.getResultInstructions(
                executionContext,
                serviceExecutionContext,
                executionBlueprint,
                service,
                overallField,
                underlyingParentField,
                result,
                state,
                nodes,
            )

            // then
            assert(getResultInstructions === expectedResult)

            verify(exactly = 1) {
                compat.getResultInstructions(
                    executionContext,
                    serviceExecutionContext,
                    executionBlueprint,
                    service,
                    overallField,
                    underlyingParentField,
                    result,
                    state,
                    nodes,
                )
            }

            confirmVerified(compat)
        }
    }
})
