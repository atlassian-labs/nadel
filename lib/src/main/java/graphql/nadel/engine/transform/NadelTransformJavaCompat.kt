package graphql.nadel.engine.transform

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.query.NadelQueryTransformerJavaCompat
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.normalized.ExecutableNormalizedField
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.asDeferred
import java.util.concurrent.CompletableFuture

/**
 * See [NadelTransform]
 */
interface NadelTransformJavaCompat<State : Any> {
    /**
     * See [NadelTransform.isApplicable]
     */
    fun isApplicable(
        executionContext: NadelExecutionContext,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): CompletableFuture<State?>

    /**
     * See [NadelTransform.transformField]
     */
    fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformerJavaCompat,
        service: Service,
        field: ExecutableNormalizedField,
        state: State,
    ): CompletableFuture<NadelTransformFieldResult>

    /**
     * See [NadelTransform.getResultInstructions]
     */
    fun getResultInstructions(
        executionContext: NadelExecutionContext,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: State,
    ): CompletableFuture<List<NadelResultInstruction>>

    companion object {
        @JvmStatic
        fun <State : Any> create(compat: NadelTransformJavaCompat<State>): NadelTransform<State> {
            return object : NadelTransform<State> {
                override suspend fun isApplicable(
                    executionContext: NadelExecutionContext,
                    services: Map<String, Service>,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): State? {
                    return compat.isApplicable(
                        executionContext = executionContext,
                        services = services,
                        service = service,
                        overallField = overallField,
                        hydrationDetails = hydrationDetails,
                    ).asDeferred().await()
                }

                override suspend fun transformField(
                    executionContext: NadelExecutionContext,
                    transformer: NadelQueryTransformer,
                    service: Service,
                    field: ExecutableNormalizedField,
                    state: State,
                ): NadelTransformFieldResult {
                    return coroutineScope {
                        val scope = this@coroutineScope

                        compat.transformField(
                            executionContext = executionContext,
                            transformer = NadelQueryTransformerJavaCompat(transformer, scope),
                                service = service,
                            field = field,
                            state = state,
                        ).asDeferred().await()
                    }
                }

                override suspend fun getResultInstructions(
                    executionContext: NadelExecutionContext,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    underlyingParentField: ExecutableNormalizedField?,
                    result: ServiceExecutionResult,
                    state: State,
                    nodes: JsonNodes,
                ): List<NadelResultInstruction> {
                    return compat.getResultInstructions(
                        executionContext = executionContext,
                        service = service,
                        overallField = overallField,
                        underlyingParentField = underlyingParentField,
                        result = result,
                        state = state,
                    ).asDeferred().await()
                }
            }
        }
    }
}
