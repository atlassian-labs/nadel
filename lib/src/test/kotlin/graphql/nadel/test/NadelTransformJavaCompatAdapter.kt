package graphql.nadel.test

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformJavaCompat
import graphql.nadel.engine.transform.NadelTransformServiceExecutionContext
import graphql.nadel.engine.transform.query.NadelQueryTransformerJavaCompat
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.normalized.ExecutableNormalizedField
import java.util.concurrent.CompletableFuture

interface NadelTransformJavaCompatAdapter : NadelTransformJavaCompat<Any> {
    override fun isApplicable(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): CompletableFuture<Any?> {
        return CompletableFuture.completedFuture(Unit)
    }

    override fun transformField(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        transformer: NadelQueryTransformerJavaCompat,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: Any,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
    ): CompletableFuture<NadelTransformFieldResult> {
        return CompletableFuture.completedFuture(NadelTransformFieldResult.unmodified(field))
    }

    override fun getResultInstructions(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: Any,
        nodes: JsonNodes,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
    ): CompletableFuture<List<NadelResultInstruction>> {
        return CompletableFuture.completedFuture(emptyList())
    }
}
