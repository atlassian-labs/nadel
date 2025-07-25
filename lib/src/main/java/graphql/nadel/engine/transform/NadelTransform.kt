package graphql.nadel.engine.transform

import graphql.language.Directive
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.normalized.ExecutableNormalizedField

interface NadelTransform<State : Any> {
    /**
     * The name of the transform. Used for metrics purposes. Should be short and contain no special characters.
     */
    val name: String
        get() = javaClass.simpleName.ifBlank { "UnknownTransform" }

    /**
     * This method is called once before execution of the transform starts.
     * Override it to create a common object that is shared between all invocations of all other methods
     * of the transform on all the fields.
     *
     * @param executionBlueprint the [NadelOverallExecutionBlueprint] of the Nadel instance being operated on
     * @param rootField the root [ExecutableNormalizedField] of the operation this [NadelTransform] runs on
     * @param hydrationDetails the [ServiceExecutionHydrationDetails] when the [NadelTransform] is applied to fields inside
     * hydrations, `null` otherwise
     *
     * @return a common [NadelTransformServiceExecutionContext] that will be fed into all other methods of this transform
     * when they run or [null] if you don't need such functionality
     *
     */
    suspend fun buildContext(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        rootField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): NadelTransformServiceExecutionContext? {
        return null
    }

    /**
     * Determines whether the [NadelTransform] should run. If it should run return a [State].
     *
     * The returned [State] is then fed into [transformField] and [getResultInstructions].
     *
     * So here you will want to check whether the [overallField] has a specific [Directive] or
     * if the field has an instruction inside [NadelOverallExecutionBlueprint] etc.
     *
     * The state should hold data that is shared between [transformField] and [getResultInstructions]
     * e.g. the names of fields that will be added etc. The implementation of [State] is completely up
     * to you. You can make it mutable if that makes your life easier etc.
     *
     * Note: a transform is applied to all fields recursively
     *
     * @param executionBlueprint the [NadelOverallExecutionBlueprint] of the Nadel instance being operated on
     * @param service the [Service] the [overallField] belongs to
     * @param overallField the [ExecutableNormalizedField] in question, we are asking whether it [isApplicable] for transforms
     * @param hydrationDetails the [ServiceExecutionHydrationDetails] when the [NadelTransform] is applied to fields inside
     * hydrations, `null` otherwise
     *
     * @return null if the [NadelTransform] should not run, non-null [State] otherwise
     */
    suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
        hydrationDetails: ServiceExecutionHydrationDetails? = null,
    ): State?

    /**
     * Override this function to rewrite the result. If you do not wish to rewrite the field,
     * simply return [NadelTransformFieldResult.unmodified].
     *
     * This lets you transform a field. You may add extra fields, modify the [field], or
     * ever delete the [field] from the query. See [NadelTransformFieldResult] for more.
     */
    suspend fun transformField(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: State,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
    ): NadelTransformFieldResult

    /**
     * Override this function to rewrite the result. If you do not wish to rewrite the result,
     * simply return [emptyList].
     *
     * Return a [List] of [NadelResultInstruction]s to modify the result.
     */
    suspend fun getResultInstructions(
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
    ): List<NadelResultInstruction>

    /**
     * Called once after all other functions of a transform ran on all fields in the query.
     * Override this function to perform cleanup or finalization tasks.
     * This method is optional for implementing classes.
     */
    suspend fun onComplete(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        result: ServiceExecutionResult,
        nodes: JsonNodes,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
    ) {
    }
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
