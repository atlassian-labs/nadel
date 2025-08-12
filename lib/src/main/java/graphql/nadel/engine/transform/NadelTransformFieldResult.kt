package graphql.nadel.engine.transform

data class NadelTransformFieldResult(
    /**
     * The original field given in [NadelTransform.transformField].
     *
     * Set to null if you want to delete the field
     */
    val newField: graphql.normalized.ExecutableNormalizedField?,
    /**
     * Any additional artificial fields you want to add to the query for
     * transformation purposes.
     *
     * These will never be presented in the overall result. Any fields here
     * will be automatically removed by Nadel as GraphQL only allows for
     * fields specified by the incoming query to be in the result.
     */
    val artificialFields: List<graphql.normalized.ExecutableNormalizedField> = emptyList(),
) {
    companion object {
        /**
         * Idiomatic helper for saying you didn't modify the field.
         */
        fun unmodified(field: graphql.normalized.ExecutableNormalizedField): NadelTransformFieldResult {
            return NadelTransformFieldResult(newField = field)
        }
    }
}
