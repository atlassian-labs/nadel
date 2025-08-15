package graphql.nadel.engine.transform.query

import graphql.nadel.engine.instrumentation.NadelInstrumentationTimer
import graphql.nadel.engine.plan.NadelExecutionPlan
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField

class NadelQueryTransformer private constructor(
    private val executionPlan: NadelExecutionPlan,
    private val timer: NadelInstrumentationTimer.BatchTimer,
) {
    companion object {
        internal suspend fun transformQuery(
            executionPlan: NadelExecutionPlan,
            field: ExecutableNormalizedField,
        ): TransformResult {
            executionPlan.operationExecutionContext.timer.batch().use { timer ->
                val transformer = NadelQueryTransformer(
                    executionPlan,
                    timer,
                )
                val result = transformer.transform(field)
                    .also { rootFields ->
                        transformer.fixParentRefs(parent = null, rootFields)
                    }

                return TransformResult(
                    result = result,
                    artificialFields = transformer.artificialFields,
                    overallToUnderlyingFields = transformer.overallToUnderlyingFields,
                )
            }
        }
    }

    private val artificialFields: MutableList<ExecutableNormalizedField> = mutableListOf()
    private val overallToUnderlyingFields: MutableMap<ExecutableNormalizedField, MutableList<ExecutableNormalizedField>> =
        mutableMapOf()

    data class TransformResult(
        /**
         * The transformed fields.
         */
        val result: List<ExecutableNormalizedField>,
        /**
         * A list of fields that were added to the query that do not belong in the overall result.
         */
        val artificialFields: List<ExecutableNormalizedField>,
        val overallToUnderlyingFields: Map<ExecutableNormalizedField, List<ExecutableNormalizedField>>,
    )

    /**
     * Helper for calling [transform] for all the given [fields].
     */
    suspend fun transform(
        fields: List<ExecutableNormalizedField>,
    ): List<ExecutableNormalizedField> {
        return fields.flatMap {
            transform(it)
        }
    }

    suspend fun transform(
        field: ExecutableNormalizedField,
    ): List<ExecutableNormalizedField> {
        val transformationTransformFieldSteps: List<NadelExecutionPlan.TransformFieldStep> =
            executionPlan.transformFieldSteps[field]
                ?: return listOf(
                    transformPlain(field)
                )

        return transform(field, transformationTransformFieldSteps)
    }

    private suspend fun transform(
        field: ExecutableNormalizedField,
        transformFieldSteps: List<NadelExecutionPlan.TransformFieldStep>,
    ): List<ExecutableNormalizedField> {
        val transformResult = applyTransformationSteps(field, transformFieldSteps)

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

        this.artificialFields.addAll(artificialFields)

        // Track overall -> underlying fields
        this.overallToUnderlyingFields
            .computeIfAbsent(field) {
                mutableListOf()
            }
            .also {
                it.addAll(newField)
                it.addAll(artificialFields)
            }

        return if (artificialFields.isEmpty()) {
            newField
        } else {
            newField + artificialFields
        }
    }

    /**
     * Transforms a field with no [NadelTransform]s associated with it.
     */
    private suspend fun transformPlain(field: ExecutableNormalizedField): ExecutableNormalizedField {
        return field.toBuilder()
            .clearObjectTypesNames()
            .objectTypeNames(getUnderlyingTypeNames(field.objectTypeNames))
            .children(transform(field.children))
            .build()
            .also { newField ->
                // Track overall -> underlying fields
                this.overallToUnderlyingFields
                    .computeIfAbsent(field) {
                        mutableListOf()
                    }
                    .also {
                        it.add(newField)
                    }
            }
    }

    private suspend fun applyTransformationSteps(
        field: ExecutableNormalizedField,
        transformFieldSteps: List<NadelExecutionPlan.TransformFieldStep>,
    ): NadelTransformFieldResult {
        var newField: ExecutableNormalizedField = field
        val artificialFields = mutableListOf<ExecutableNormalizedField>()

        for (step in transformFieldSteps) {
            val transformResultForStep = timer.time(step.timingSteps.queryTransform) {
                step.transform.transformField(
                    transformContext = step.transformFieldContext,
                    transformer = this,
                    field = newField,
                )
            }
            artificialFields.addAll(transformResultForStep.artificialFields)
            newField = transformResultForStep.newField
                ?: return NadelTransformFieldResult(null, artificialFields)
        }

        return NadelTransformFieldResult(
            newField = newField,
            artificialFields = artificialFields,
        )
    }

    private fun getUnderlyingTypeNames(objectTypeNames: Collection<String>): List<String> {
        val executionContext = executionPlan.operationExecutionContext.executionContext
        val service = executionPlan.operationExecutionContext.service

        return if (executionContext.hints.sharedTypeRenames(service)) {
            objectTypeNames.map {
                executionContext.executionBlueprint.getUnderlyingTypeName(overallTypeName = it)
            }
        } else {
            objectTypeNames.map {
                executionContext.executionBlueprint.getUnderlyingTypeName(service, overallTypeName = it)
            }
        }
    }

    private fun fixParentRefs(
        parent: ExecutableNormalizedField?,
        transformFields: List<ExecutableNormalizedField>,
    ) {
        transformFields.forEach {
            it.replaceParent(parent)
            fixParentRefs(parent = it, it.children)
        }
    }
}
