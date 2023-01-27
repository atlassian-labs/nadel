package graphql.nadel.engine.transform.query

import graphql.nadel.NadelEngineContext
import graphql.nadel.Service
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.plan.NadelExecutionPlan
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformContext
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField as ENF

private data class NadelQueryTransformerContext(
    val service: Service,
    val executionPlan: NadelExecutionPlan,

    val artificialFields: MutableList<ENF>,
    val overallToUnderlyingFields: MutableMap<ENF, List<ENF>>,
)

context(NadelEngineContext, NadelExecutionContext, NadelQueryTransformerContext)
class NadelQueryTransformer private constructor() {
    companion object {
        context(NadelEngineContext, NadelExecutionContext)
        suspend fun transformQuery(
            service: Service,
            executionPlan: NadelExecutionPlan,

            field: ENF,
        ): TransformResult {
            val transformContext = NadelQueryTransformerContext(
                service = service,
                executionPlan = executionPlan,
                artificialFields = mutableListOf(),
                overallToUnderlyingFields = mutableMapOf(),
            )

            val result = with(transformContext) {
                val transformer = NadelQueryTransformer()
                transformer.transform(field)
                    .also { rootFields ->
                        transformer.fixParentRefs(parent = null, rootFields)
                    }
            }

            return TransformResult(
                result = result,
                artificialFields = transformContext.artificialFields,
                overallToUnderlyingFields = transformContext.overallToUnderlyingFields,
            )
        }
    }

    data class TransformResult(
        /**
         * The transformed fields.
         */
        val result: List<ENF>,
        /**
         * A list of fields that were added to the query that do not belong in the overall result.
         */
        val artificialFields: List<ENF>,
        val overallToUnderlyingFields: Map<ENF, List<ENF>>,
    )

    fun markArtificial(field: ENF) {
        artificialFields.add(field)
    }

    private fun trackArtificialFields(fields: List<ENF>) {
        artificialFields.addAll(fields)
    }

    /**
     * Helper for calling [transform] for all the given [fields].
     */
    suspend fun transform(
        fields: List<ENF>,
    ): List<ENF> {
        return fields.flatMap {
            transform(it)
        }
    }

    suspend fun transform(
        field: ENF,
    ): List<ENF> {
        val transformationSteps: List<NadelExecutionPlan.Step<NadelTransformContext>> =
            executionPlan.transformationSteps[field]
                ?: return listOf(
                    transformPlain(field)
                )

        return transform(field, transformationSteps)
    }

    private suspend fun transform(
        field: ENF,
        transformationSteps: List<NadelExecutionPlan.Step<NadelTransformContext>>,
    ): List<ENF> {
        val transformResult = applyTransformationSteps(field, transformationSteps)

        val artificialFields = transformResult.artificialFields.map {
            it.toBuilder()
                .clearObjectTypesNames()
                .objectTypeNames(getUnderlyingTypeNames(it.objectTypeNames))
                .build()
        }

        val newField = listOfNotNull(
            transformResult.newField?.let {
                it.toBuilder()
                    .clearObjectTypesNames()
                    .objectTypeNames(getUnderlyingTypeNames(it.objectTypeNames))
                    .children(transform(it.children))
                    .build()
            },
        )

        trackArtificialFields(artificialFields)

        // Track overall -> underlying fields
        overallToUnderlyingFields.compute(field) { _, oldValue ->
            (oldValue ?: emptyList()) + newField + artificialFields
        }

        return artificialFields + newField
    }

    /**
     * Transforms a field with no [NadelTransform]s associated with it.
     */
    private suspend fun transformPlain(field: ENF): ENF {
        return field.toBuilder()
            .clearObjectTypesNames()
            .objectTypeNames(getUnderlyingTypeNames(field.objectTypeNames))
            .children(transform(field.children))
            .build()
            .also { newField ->
                // Track overall -> underlying fields
                overallToUnderlyingFields.compute(field) { _, oldValue ->
                    (oldValue ?: emptyList()) + newField
                }
            }
    }

    private suspend fun applyTransformationSteps(
        field: ENF,
        transformationSteps: List<NadelExecutionPlan.Step<NadelTransformContext>>,
    ): NadelTransformFieldResult {
        var fieldFromPreviousTransform: ENF = field
        var aggregatedTransformResult: NadelTransformFieldResult? = null
        for ((_, _, transform, state) in transformationSteps) {
            val transformResultForStep = with(state) {
                transform.transformField(
                    this@NadelQueryTransformer,
                    fieldFromPreviousTransform,
                )
            }
            aggregatedTransformResult = if (aggregatedTransformResult == null) {
                transformResultForStep
            } else {
                NadelTransformFieldResult(
                    transformResultForStep.newField,
                    aggregatedTransformResult.artificialFields + transformResultForStep.artificialFields,
                )
            }
            fieldFromPreviousTransform = transformResultForStep.newField ?: break
        }
        return aggregatedTransformResult!!
    }

    private fun getUnderlyingTypeNames(objectTypeNames: Collection<String>): List<String> {
        return objectTypeNames.map {
            executionBlueprint.getUnderlyingTypeName(service, overallTypeName = it)
        }
    }

    private fun fixParentRefs(
        parent: ENF?,
        transformFields: List<ENF>,
    ) {
        transformFields.forEach {
            it.replaceParent(parent)
            fixParentRefs(parent = it, it.children)
        }
    }
}
