package graphql.nadel.engine.transform

import graphql.language.Directive
import graphql.nadel.NadelEngineContext
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.normalized.ExecutableNormalizedField

interface NadelTransformContext

interface NadelTransform<TransformContext : NadelTransformContext> {
    /**
     * The name of the transform. Used for metrics purposes. Should be short and contain no special characters.
     */
    val name: String
        get() = javaClass.simpleName.ifBlank { "UnknownTransform" }

    /**
     * Determines whether the [NadelTransform] should run. If it should run return a [TransformContext].
     *
     * The returned [TransformContext] is then fed into [transformField] and [getResultInstructions].
     *
     * So here you will want to check whether the [overallField] has a specific [Directive] or
     * if the field has an instruction inside [NadelOverallExecutionBlueprint] etc.
     *
     * The state should hold data that is shared between [transformField] and [getResultInstructions]
     * e.g. the names of fields that will be added etc. The implementation of [TransformContext] is completely up
     * to you. You can make it mutable if that makes your life easier etc.
     *
     * @param service the [Service] the [overallField] belongs to
     * @param overallField the [ExecutableNormalizedField] in question, we are asking whether it [isApplicable] for transforms
     * @param hydrationDetails the [ServiceExecutionHydrationDetails] when the [NadelTransform] is applied to fields inside
     * hydrations, `null` otherwise
     *
     * @return null if the [NadelTransform] should not run, non-null [TransformContext] otherwise
     */
    context(NadelEngineContext, NadelExecutionContext)
    suspend fun isApplicable(
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails? = null,
    ): TransformContext?

    /**
     * Override this function to rewrite the result. If you do not wish to rewrite the field,
     * simply return [NadelTransformFieldResult.unmodified].
     *
     * This lets you transform a field. You may add extra fields, modify the [field], or
     * ever delete the [field] from the query. See [NadelTransformFieldResult] for more.
     */
    context(NadelEngineContext, NadelExecutionContext, TransformContext)
    suspend fun transformField(
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult

    /**
     * Override this function to rewrite the result. If you do not wish to rewrite the result,
     * simply return [emptyList].
     *
     * Return a [List] of [NadelResultInstruction]s to modify the result.
     */
    context(NadelEngineContext, NadelExecutionContext, TransformContext)
    suspend fun getResultInstructions(
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        nodes: JsonNodes,
    ): List<NadelResultInstruction>
}

data class NadelTransformFieldResult(
    /**
     * The original field given in [NadelTransform.transformField].
     *
     * Set to null if you want to delete the field
     */
    val newField: ExecutableNormalizedField?,
    /**
     * Any additional artificial fields you want to add to the query for
     * transformation purposes.
     *
     * These will never be presented in the overall result. Any fields here
     * will be automatically removed by Nadel as GraphQL only allows for
     * fields specified by the incoming query to be in the result.
     */
    val artificialFields: List<ExecutableNormalizedField> = emptyList(),
) {
    companion object {
        /**
         * Idiomatic helper for saying you didn't modify the field.
         */
        fun unmodified(field: ExecutableNormalizedField): NadelTransformFieldResult {
            return NadelTransformFieldResult(newField = field)
        }
    }
}
