package graphql.nadel.test

import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.transform.NadelTransformFieldContext
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformJavaCompat
import graphql.nadel.engine.transform.NadelTransformOperationContext
import graphql.nadel.engine.transform.query.NadelQueryTransformerJavaCompat
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.test.NadelTransformJavaCompatAdapter.TransformFieldContext
import graphql.nadel.test.NadelTransformJavaCompatAdapter.TransformOperationContext
import graphql.normalized.ExecutableNormalizedField
import java.util.concurrent.CompletableFuture

interface NadelTransformJavaCompatAdapter : NadelTransformJavaCompat<TransformOperationContext, TransformFieldContext> {
    open class TransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
    ) : NadelTransformOperationContext()

    open class TransformFieldContext(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
    ) : NadelTransformFieldContext<TransformOperationContext>()

    override fun getTransformOperationContext(
        operationExecutionContext: NadelOperationExecutionContext,
    ): CompletableFuture<TransformOperationContext> {
        return CompletableFuture.completedFuture(TransformOperationContext(operationExecutionContext))
    }

    override fun getTransformFieldContext(
        transformContext: TransformOperationContext,
        overallField: ExecutableNormalizedField,
    ): CompletableFuture<TransformFieldContext?> {
        return CompletableFuture.completedFuture(TransformFieldContext(transformContext, overallField))
    }

    override fun transformField(
        transformContext: TransformFieldContext,
        transformer: NadelQueryTransformerJavaCompat,
        field: ExecutableNormalizedField,
    ): CompletableFuture<NadelTransformFieldResult> {
        return CompletableFuture.completedFuture(NadelTransformFieldResult.unmodified(field))
    }

    override fun transformResult(
        transformContext: TransformFieldContext,
        underlyingParentField: ExecutableNormalizedField?,
        resultNodes: JsonNodes,
    ): CompletableFuture<List<NadelResultInstruction>> {
        return CompletableFuture.completedFuture(emptyList())
    }

    override fun onComplete(
        transformContext: TransformOperationContext,
        resultNodes: JsonNodes,
    ): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }
}
