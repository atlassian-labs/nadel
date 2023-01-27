package graphql.nadel.test

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformJavaCompat
import graphql.nadel.engine.transform.NadelTransformState
import graphql.nadel.engine.transform.query.NadelQueryTransformerJavaCompat
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.test.NadelTransformJavaCompatAdapter.State
import graphql.normalized.ExecutableNormalizedField
import java.util.concurrent.CompletableFuture

interface NadelTransformJavaCompatAdapter : NadelTransformJavaCompat<State> {
    object State : NadelTransformState

    override fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): CompletableFuture<State?> {
        return CompletableFuture.completedFuture(State)
    }

    override fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformerJavaCompat,
        executionBlueprint: NadelOverallExecutionBlueprint,
        field: ExecutableNormalizedField,
        state: State,
    ): CompletableFuture<NadelTransformFieldResult> {
        return CompletableFuture.completedFuture(NadelTransformFieldResult.unmodified(field))
    }

    override fun getResultInstructions(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: State,
        nodes: JsonNodes,
    ): CompletableFuture<List<NadelResultInstruction>> {
        return CompletableFuture.completedFuture(emptyList())
    }
}
