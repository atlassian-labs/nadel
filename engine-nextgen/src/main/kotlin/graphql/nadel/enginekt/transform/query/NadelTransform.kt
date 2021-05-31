package graphql.nadel.enginekt.transform.query

import graphql.language.Directive
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

internal typealias AnyNadelTransform = NadelTransform<Any>

internal interface NadelTransform<State : Any> {
    /**
     * Determines whether the [NadelTransform] should run. If it should run return a [State].
     *
     * The returned [State] is then fed into [transformField] and [getResultInstructions].
     *
     * So here you will want to check whether the [field] has a specific [Directive] or
     * if the field has an instruction inside [NadelExecutionBlueprint] etc.
     *
     * The state should hold data that is shared between [transformField] and [getResultInstructions]
     * e.g. the names of fields that will be added etc. The implementation of [State] is completely up
     * to you. You can make it mutable if that makes your life easier etc.
     *
     * @return null if the [NadelTransform] should not run, non-null [State] otherwise
     */
    suspend fun isApplicable(
        userContext: Any?,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        service: Service,
        field: NormalizedField,
    ): State?

    /**
     * Override this function to rewrite the result. If you do not wish to rewrite the field,
     * simply return [NadelTransformFieldResult.unmodified].
     *
     * This lets you transform a field. You may add extra fields, modify the [field], or
     * ever delete the [field] from the query. See [NadelTransformFieldResult] for more.
     */
    suspend fun transformField(
        transformer: NadelQueryTransformer.Continuation,
        service: Service,
        overallSchema: GraphQLSchema,
        executionPlan: NadelExecutionPlan,
        field: NormalizedField,
        state: State,
    ): NadelTransformFieldResult

    /**
     * Override this function to rewrite the result. If you do not wish to rewrite the result,
     * simply return [emptyList].
     *
     * Return a [List] of [NadelResultInstruction]s to modify the result.
     */
    suspend fun getResultInstructions(
        userContext: Any?,
        overallSchema: GraphQLSchema,
        executionPlan: NadelExecutionPlan,
        service: Service,
        field: NormalizedField,
        result: ServiceExecutionResult,
        state: State,
    ): List<NadelResultInstruction>
}

data class NadelTransformFieldResult(
    /**
     * The original field given in [NadelTransform.transformField].
     *
     * Set to null if you want to delete the field
     */
    val newField: NormalizedField?,
    /**
     * Any additional artificial fields you want to add to the query for
     * transformation purposes.
     *
     * These will never be presented in the overall result. Any fields here
     * will be automatically removed by Nadel as GraphQL only allows for
     * fields specified by the incoming query to be in the result.
     */
    val extraFields: List<NormalizedField> = emptyList(),
) {
    companion object {
        /**
         * Idiomatic helper for saying you didn't modify the field.
         */
        fun unmodified(field: NormalizedField): NadelTransformFieldResult {
            return NadelTransformFieldResult(newField = field)
        }
    }
}
