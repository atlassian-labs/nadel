package graphql.nadel.transform

import io.kotest.core.spec.style.DescribeSpec

class NadelTransformJavaCompatTest : DescribeSpec({
    // describe("create compat") {
    //     it("delegates name to compat") {
    //         // given
    //         val compat = spy(object : NadelTransformJavaCompatAdapter {
    //             override val name: String
    //                 get() = "Test"
    //         })
    //
    //         val transformer = NadelTransformJavaCompat.create(compat)
    //
    //         // when
    //         val name = transformer.name
    //
    //         // then
    //         assert(name == "Test")
    //         verify(exactly = 1) { compat.name }
    //
    //         confirmVerified(compat)
    //     }
    //
    //     it("delegates isApplicable to compat") {
    //         // given
    //         val compat = spy(object : NadelTransformJavaCompatAdapter {
    //             override val name: String
    //                 get() = "Test"
    //
    //             override fun isApplicable(
    //                 executionContext: NadelExecutionContext,
    //                 executionBlueprint: NadelOverallExecutionBlueprint,
    //                 services: Map<String, Service>,
    //                 service: Service,
    //                 overallField: ExecutableNormalizedField,
    //                 hydrationDetails: ServiceExecutionHydrationDetails?,
    //             ): CompletableFuture<Any?> {
    //                 return CompletableFuture.completedFuture(123)
    //             }
    //         })
    //
    //         val transformer = NadelTransformJavaCompat.create(compat)
    //
    //         val executionContext = mock<NadelExecutionContext>()
    //         val executionBlueprint = mock<NadelOverallExecutionBlueprint>()
    //         val services = mock<Map<String, Service>>()
    //         val service = mock<Service>()
    //         val overallField = mock<ExecutableNormalizedField>()
    //         val hydrationDetails = mock<ServiceExecutionHydrationDetails>()
    //
    //         // when
    //         val isApplicable = transformer.isApplicable(
    //             executionContext,
    //             executionBlueprint,
    //             services,
    //             service,
    //             overallField,
    //             hydrationDetails,
    //         )
    //
    //         // then
    //         assert(isApplicable == 123)
    //
    //         verify(exactly = 1) {
    //             compat.isApplicable(
    //                 executionContext,
    //                 executionBlueprint,
    //                 services,
    //                 service,
    //                 overallField,
    //                 hydrationDetails,
    //             )
    //         }
    //
    //         confirmVerified(compat)
    //     }
    //
    //     it("delegates transformField to compat") {
    //         // given
    //         val expectedResult = mock<NadelTransformFieldResult>()
    //         val compat = spy(object : NadelTransformJavaCompatAdapter {
    //             override val name: String
    //                 get() = "Test"
    //
    //             override fun transformField(
    //                 executionContext: NadelExecutionContext,
    //                 transformer: NadelQueryTransformerJavaCompat,
    //                 executionBlueprint: NadelOverallExecutionBlueprint,
    //                 service: Service,
    //                 field: ExecutableNormalizedField,
    //                 state: Any,
    //             ): CompletableFuture<NadelTransformFieldResult> {
    //                 return CompletableFuture.completedFuture(expectedResult)
    //             }
    //         })
    //
    //         val transformer = NadelTransformJavaCompat.create(compat)
    //
    //         val executionContext = mock<NadelExecutionContext>()
    //         val queryTransformer = mock<NadelQueryTransformer>()
    //         val executionBlueprint = mock<NadelOverallExecutionBlueprint>()
    //         val service = mock<Service>()
    //         val field = mock<ExecutableNormalizedField>()
    //         val state = mock<Any>()
    //
    //         // when
    //         val transformField = transformer.transformField(
    //             executionContext,
    //             queryTransformer,
    //             executionBlueprint,
    //             service,
    //             field,
    //             state,
    //         )
    //
    //         // then
    //         assert(transformField === expectedResult)
    //
    //         verify(exactly = 1) {
    //             compat.transformField(
    //                 executionContext,
    //                 // This is another wrapper for Java compat
    //                 match { it.queryTransformer == queryTransformer },
    //                 executionBlueprint,
    //                 service,
    //                 field,
    //                 state,
    //             )
    //         }
    //
    //         confirmVerified(compat)
    //     }
    //
    //     it("delegates getResultInstructions to compat") {
    //         // given
    //         val expectedResult = listOf<NadelResultInstruction>(
    //             NadelResultInstruction.Set(
    //                 subject = JsonNode(mutableMapOf<String, Any?>()),
    //                 key = NadelResultKey("hello"),
    //                 newValue = JsonNode(1),
    //             )
    //         )
    //         val compat = spy(object : NadelTransformJavaCompatAdapter {
    //             override val name: String
    //                 get() = "Test"
    //
    //             override fun getResultInstructions(
    //                 executionContext: NadelExecutionContext,
    //                 executionBlueprint: NadelOverallExecutionBlueprint,
    //                 service: Service,
    //                 overallField: ExecutableNormalizedField,
    //                 underlyingParentField: ExecutableNormalizedField?,
    //                 result: ServiceExecutionResult,
    //                 state: Any,
    //                 nodes: JsonNodes,
    //             ): CompletableFuture<List<NadelResultInstruction>> {
    //                 return CompletableFuture.completedFuture(expectedResult)
    //             }
    //         })
    //
    //         val transformer = NadelTransformJavaCompat.create(compat)
    //
    //         val executionContext = mock<NadelExecutionContext>()
    //         val executionBlueprint = mock<NadelOverallExecutionBlueprint>()
    //         val service = mock<Service>()
    //         val overallField = mock<ExecutableNormalizedField>()
    //         val underlyingParentField = mock<ExecutableNormalizedField>()
    //         val result = mock<ServiceExecutionResult>()
    //         val state = mock<Any>()
    //         val nodes = mock<JsonNodes>()
    //
    //         // when
    //         val getResultInstructions = transformer.getResultInstructions(
    //             executionContext,
    //             executionBlueprint,
    //             service,
    //             overallField,
    //             underlyingParentField,
    //             result,
    //             state,
    //             nodes,
    //         )
    //
    //         // then
    //         assert(getResultInstructions === expectedResult)
    //
    //         verify(exactly = 1) {
    //             compat.getResultInstructions(
    //                 executionContext,
    //                 executionBlueprint,
    //                 service,
    //                 overallField,
    //                 underlyingParentField,
    //                 result,
    //                 state,
    //                 nodes,
    //             )
    //         }
    //
    //         confirmVerified(compat)
    //     }
    // }
})
