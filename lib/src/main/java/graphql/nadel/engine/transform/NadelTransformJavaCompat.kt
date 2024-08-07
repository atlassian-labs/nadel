package graphql.nadel.engine.transform

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.engine.NadelExecutionContext
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
     * See [NadelTransform.isApplicable]
     */
    fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
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
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: State,
    ): CompletableFuture<NadelTransformFieldResult>

    /**
     * See [NadelTransform.getResultInstructions]
     */
    fun getResultInstructions(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        state: State,
        nodes: JsonNodes,
    ): CompletableFuture<List<NadelResultInstruction>>

    companion object {
        @JvmStatic
        fun <State : Any> create(compat: NadelTransformJavaCompat<State>): NadelTransform<State> {
            return object : NadelTransform<State> {
                override val name: String
                    get() = compat.name

                override suspend fun isApplicable(
                    executionContext: NadelExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    services: Map<String, Service>,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): State? {
                    return compat.isApplicable(
                        executionContext = executionContext,
                        executionBlueprint = executionBlueprint,
                        services = services,
                        service = service,
                        overallField = overallField,
                        hydrationDetails = hydrationDetails,
                    ).asDeferred().await()
                }

                override suspend fun transformField(
                    executionContext: NadelExecutionContext,
                    transformer: NadelQueryTransformer,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    service: Service,
                    field: ExecutableNormalizedField,
                    state: State,
                ): NadelTransformFieldResult {
                    return coroutineScope {
                        val scope = this@coroutineScope

                        compat.transformField(
                            executionContext = executionContext,
                            transformer = NadelQueryTransformerJavaCompat(transformer, scope),
                            executionBlueprint = executionBlueprint,
                            service = service,
                            field = field,
                            state = state,
                        ).asDeferred().await()
                    }
                }

                override suspend fun getResultInstructions(
                    executionContext: NadelExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    underlyingParentField: ExecutableNormalizedField?,
                    state: State,
                    nodes: JsonNodes,
                ): List<NadelResultInstruction> {
                    return compat.getResultInstructions(
                        executionContext = executionContext,
                        executionBlueprint = executionBlueprint,
                        service = service,
                        overallField = overallField,
                        underlyingParentField = underlyingParentField,
                        state = state,
                        nodes = nodes,
                    ).asDeferred().await()
                }
            }
        }
    }
}
