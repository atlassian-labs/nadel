package graphql.nadel.engine.transform

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
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
    val name: String
        get() = javaClass.simpleName.ifBlank { "UnknownTransform" }

    /**
     * See [NadelTransform.buildContext]
     */
    fun buildContext(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        rootField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): CompletableFuture<NadelTransformServiceExecutionContext?> {
        return CompletableFuture.completedFuture(null)
    }

    /**
     * See [NadelTransform.isApplicable]
     *
     * Note: a transform is applied to all fields recursively
     */
    fun isApplicable(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): CompletableFuture<State?>

    /**
     * See [NadelTransform.transformField]
     */
    fun transformField(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        transformer: NadelQueryTransformerJavaCompat,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: State,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
    ): CompletableFuture<NadelTransformFieldResult>

    /**
     * See [NadelTransform.getResultInstructions]
     */
    fun getResultInstructions(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: State,
        nodes: JsonNodes,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
    ): CompletableFuture<List<NadelResultInstruction>>

    /**
     * See [NadelTransform.onComplete]
     */
    fun onComplete(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        result: ServiceExecutionResult,
        nodes: JsonNodes,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
    ): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }

    companion object {
        @JvmStatic
        fun <State : Any> create(
            compat: NadelTransformJavaCompat<State>,
        ): NadelTransform<State> {
            return object : NadelTransform<State> {
                override val name: String
                    get() = compat.name

                override suspend fun buildContext(
                    executionContext: NadelExecutionContext,
                    serviceExecutionContext: NadelServiceExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    services: Map<String, Service>,
                    service: Service,
                    rootField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): NadelTransformServiceExecutionContext? {
                    return compat.buildContext(
                        executionContext,
                        serviceExecutionContext,
                        executionBlueprint,
                        services,
                        service,
                        rootField,
                        hydrationDetails
                    ).asDeferred().await()
                }

                override suspend fun isApplicable(
                    executionContext: NadelExecutionContext,
                    serviceExecutionContext: NadelServiceExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    services: Map<String, Service>,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): State? {
                    return compat.isApplicable(
                        executionContext = executionContext,
                        serviceExecutionContext = serviceExecutionContext,
                        executionBlueprint = executionBlueprint,
                        services = services,
                        service = service,
                        overallField = overallField,
                        transformServiceExecutionContext = transformServiceExecutionContext,
                        hydrationDetails = hydrationDetails,
                    ).asDeferred().await()
                }

                override suspend fun transformField(
                    executionContext: NadelExecutionContext,
                    serviceExecutionContext: NadelServiceExecutionContext,
                    transformer: NadelQueryTransformer,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    service: Service,
                    field: ExecutableNormalizedField,
                    state: State,
                    transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
                ): NadelTransformFieldResult {
                    return coroutineScope {
                        val scope = this@coroutineScope

                        compat.transformField(
                            executionContext = executionContext,
                            serviceExecutionContext = serviceExecutionContext,
                            transformer = NadelQueryTransformerJavaCompat(transformer, scope),
                            executionBlueprint = executionBlueprint,
                            service = service,
                            field = field,
                            state = state,
                            transformServiceExecutionContext = transformServiceExecutionContext
                        ).asDeferred().await()
                    }
                }

                override suspend fun getResultInstructions(
                    executionContext: NadelExecutionContext,
                    serviceExecutionContext: NadelServiceExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    underlyingParentField: ExecutableNormalizedField?,
                    result: ServiceExecutionResult,
                    state: State,
                    nodes: JsonNodes,
                    transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
                ): List<NadelResultInstruction> {
                    return compat.getResultInstructions(
                        executionContext = executionContext,
                        executionBlueprint = executionBlueprint,
                        serviceExecutionContext = serviceExecutionContext,
                        service = service,
                        overallField = overallField,
                        underlyingParentField = underlyingParentField,
                        result = result,
                        state = state,
                        nodes = nodes,
                        transformServiceExecutionContext = transformServiceExecutionContext,
                    ).asDeferred().await()
                }

                override suspend fun onComplete(
                    executionContext: NadelExecutionContext,
                    serviceExecutionContext: NadelServiceExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    service: Service,
                    result: ServiceExecutionResult,
                    nodes: JsonNodes,
                    transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
                ) {
                    compat.onComplete(
                        executionContext = executionContext,
                        serviceExecutionContext = serviceExecutionContext,
                        executionBlueprint = executionBlueprint,
                        service = service,
                        result = result,
                        nodes = nodes,
                        transformServiceExecutionContext = transformServiceExecutionContext,
                    ).asDeferred().await()
                }
            }
        }
    }
}
