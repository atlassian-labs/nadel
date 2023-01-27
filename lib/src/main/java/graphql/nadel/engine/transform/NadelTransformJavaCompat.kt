package graphql.nadel.engine.transform

import graphql.nadel.NadelEngineContext
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
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
interface NadelTransformJavaCompat<State : NadelTransformState> {
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
        field: ExecutableNormalizedField,
        state: State,
    ): CompletableFuture<NadelTransformFieldResult>

    /**
     * See [NadelTransform.getResultInstructions]
     */
    fun getResultInstructions(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: State,
        nodes: JsonNodes,
    ): CompletableFuture<List<NadelResultInstruction>>

    companion object {
        @JvmStatic
        fun <State : NadelTransformState> create(compat: NadelTransformJavaCompat<State>): NadelTransform<State> {
            return object : NadelTransform<State> {
                override val name: String
                    get() = compat.name

                context(NadelEngineContext, NadelExecutionContext)
                override suspend fun isApplicable(
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): State? {
                    return compat.isApplicable(
                        executionContext = this@NadelExecutionContext,
                        executionBlueprint = executionBlueprint,
                        services = services,
                        service = service,
                        overallField = overallField,
                        hydrationDetails = hydrationDetails,
                    ).asDeferred().await()
                }

                context(NadelEngineContext, NadelExecutionContext, State)
                override suspend fun transformField(
                    transformer: NadelQueryTransformer,
                    field: ExecutableNormalizedField,
                ): NadelTransformFieldResult {
                    return coroutineScope {
                        val scope = this@coroutineScope

                        compat.transformField(
                            executionContext = this@NadelExecutionContext,
                            transformer = NadelQueryTransformerJavaCompat(transformer, scope),
                            executionBlueprint = executionBlueprint,
                            field = field,
                            state = this@State,
                        ).asDeferred().await()
                    }
                }

                context(NadelEngineContext, NadelExecutionContext, State)
                override suspend fun getResultInstructions(
                    overallField: ExecutableNormalizedField,
                    underlyingParentField: ExecutableNormalizedField?,
                    result: ServiceExecutionResult,
                    nodes: JsonNodes,
                ): List<NadelResultInstruction> {
                    return compat.getResultInstructions(
                        executionContext = this@NadelExecutionContext,
                        executionBlueprint = executionBlueprint,
                        overallField = overallField,
                        underlyingParentField = underlyingParentField,
                        result = result,
                        state = this@State,
                        nodes = nodes,
                    ).asDeferred().await()
                }
            }
        }
    }
}
