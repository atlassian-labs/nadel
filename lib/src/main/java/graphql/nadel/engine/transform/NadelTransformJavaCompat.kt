package graphql.nadel.engine.transform

import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.query.NadelQueryTransformerJavaCompat
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.normalized.ExecutableNormalizedField
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

/**
 * See [NadelTransform]
 */
interface NadelTransformJavaCompat<
    TransformOperationContext : NadelTransformOperationContext,
    TransformFieldContext : NadelTransformFieldContext<TransformOperationContext>,
    > {
    val name: String
        get() = javaClass.simpleName.ifBlank { "UnknownTransform" }

    /**
     * See [NadelTransform.getTransformOperationContext]
     */
    fun getTransformOperationContext(
        operationExecutionContext: NadelOperationExecutionContext,
    ): CompletableFuture<TransformOperationContext>

    /**
     * See [NadelTransform.getTransformFieldContext]
     *
     * Note: a transform is applied to all fields recursively
     */
    fun getTransformFieldContext(
        transformContext: TransformOperationContext,
        overallField: ExecutableNormalizedField,
    ): CompletableFuture<TransformFieldContext?>

    /**
     * See [NadelTransform.transformField]
     */
    fun transformField(
        transformContext: TransformFieldContext,
        transformer: NadelQueryTransformerJavaCompat,
        field: ExecutableNormalizedField,
    ): CompletableFuture<NadelTransformFieldResult>

    /**
     * See [NadelTransform.transformResult]
     */
    fun transformResult(
        transformContext: TransformFieldContext,
        underlyingParentField: ExecutableNormalizedField?,
        resultNodes: JsonNodes,
    ): CompletableFuture<List<NadelResultInstruction>>

    /**
     * See [NadelTransform.onComplete]
     */
    fun onComplete(
        transformContext: TransformOperationContext,
        resultNodes: JsonNodes,
    ): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }

    companion object {
        @JvmStatic
        fun <
            TransformOperationContext : NadelTransformOperationContext,
            TransformFieldContext : NadelTransformFieldContext<TransformOperationContext>,
            > create(
            compat: NadelTransformJavaCompat<TransformOperationContext, TransformFieldContext>,
        ): NadelTransform<TransformOperationContext, TransformFieldContext> {
            return object : NadelTransform<TransformOperationContext, TransformFieldContext> {
                override val name: String
                    get() = compat.name

                override suspend fun getTransformOperationContext(
                    operationExecutionContext: NadelOperationExecutionContext,
                ): TransformOperationContext {
                    return compat.getTransformOperationContext(
                        operationExecutionContext = operationExecutionContext,
                    ).await()
                }

                override suspend fun getTransformFieldContext(
                    transformContext: TransformOperationContext,
                    overallField: ExecutableNormalizedField,
                ): TransformFieldContext? {
                    return compat.getTransformFieldContext(
                        transformContext = transformContext,
                        overallField = overallField,
                    ).await()
                }

                override suspend fun transformField(
                    transformContext: TransformFieldContext,
                    transformer: NadelQueryTransformer,
                    field: ExecutableNormalizedField,
                ): NadelTransformFieldResult {
                    return coroutineScope {
                        compat.transformField(
                            transformContext = transformContext,
                            transformer = NadelQueryTransformerJavaCompat(transformer, this),
                            field = field,
                        ).await()
                    }
                }

                override suspend fun transformResult(
                    transformContext: TransformFieldContext,
                    underlyingParentField: ExecutableNormalizedField?,
                    resultNodes: JsonNodes,
                ): List<NadelResultInstruction> {
                    return compat.transformResult(
                        transformContext = transformContext,
                        underlyingParentField = underlyingParentField,
                        resultNodes = resultNodes,
                    ).await()
                }

                override suspend fun onComplete(
                    transformContext: TransformOperationContext,
                    resultNodes: JsonNodes,
                ) {
                    compat.onComplete(
                        transformContext = transformContext,
                        resultNodes = resultNodes,
                    ).await()
                }
            }
        }
    }
}
