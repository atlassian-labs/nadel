package graphql.nadel.engine.transform

import graphql.language.Directive
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.normalized.ExecutableNormalizedField

/**
 * This is what every transform gets cast to when actually used in a generic list.
 *
 * We can't invoke functions when star projections are used, so we use the next best thing.
 */
internal typealias GenericNadelTransform = NadelTransform<
    NadelTransformOperationContext,
    NadelTransformFieldContext<NadelTransformOperationContext>,
    >

interface NadelTransform<
    TransformOperationContext : NadelTransformOperationContext,
    TransformFieldContext : NadelTransformFieldContext<TransformOperationContext>,
    > {
    /**
     * The name of the transform. Used for metrics purposes. Should be short and contain no special characters.
     */
    val name: String
        get() = javaClass.simpleName.ifBlank { "UnknownTransform" }

    /**
     * This function is called once before execution of the transform starts.
     *
     * Use it to create a common object that is shared between all invocations of all other methods
     * of the transform on all the fields.
     *
     * @return a common [NadelTransformOperationContext] that will be fed into all other methods of this transform
     */
    suspend fun getTransformOperationContext(
        operationExecutionContext: NadelOperationExecutionContext,
    ): TransformOperationContext

    /**
     * Determines whether the [NadelTransform] should run. If it should run return a [NadelTransformFieldContext].
     *
     * The returned [NadelTransformFieldContext] is passed onto [transformField] and [transformResult].
     *
     * So here you will want to check whether the [overallField] has a specific [Directive] or
     * if the field has an instruction inside [NadelOverallExecutionBlueprint] etc.
     *
     * The state should hold data that is shared between [transformField] and [transformResult]
     * e.g. the names of fields that will be added etc. The implementation of [NadelTransformFieldContext]
     * is completely up to you.
     *
     * @param overallField the [ExecutableNormalizedField] in question, we are asking whether it [getTransformFieldContext] for transforms
     * hydrations, `null` otherwise
     *
     * @return null if the [NadelTransform] should not run, non-null [State] otherwise
     */
    suspend fun getTransformFieldContext(
        transformContext: TransformOperationContext,
        overallField: ExecutableNormalizedField,
    ): TransformFieldContext?

    /**
     * Override this function to rewrite the result. If you do not wish to rewrite the field,
     * simply return [NadelTransformFieldResult.unmodified].
     *
     * This lets you transform a field. You may add extra fields, modify the [field], or
     * ever delete the [field] from the query. See [NadelTransformFieldResult] for more.
     */
    suspend fun transformField(
        transformContext: TransformFieldContext,
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult

    /**
     * Override this function to rewrite the result. If you do not wish to rewrite the result,
     * simply return [emptyList].
     *
     * Return a [List] of [NadelResultInstruction]s to modify the result.
     */
    suspend fun transformResult(
        transformContext: TransformFieldContext,
        underlyingParentField: ExecutableNormalizedField?,
        resultNodes: JsonNodes,
    ): List<NadelResultInstruction>

    /**
     * Called once after all other functions of a transform ran on all fields in the query.
     * Override this function to perform cleanup or finalization tasks.
     * This method is optional for implementing classes.
     */
    suspend fun onComplete(
        transformContext: TransformOperationContext,
        resultNodes: JsonNodes,
    ) {
    }
}
