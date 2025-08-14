package graphql.nadel.engine.transform.query

import graphql.nadel.Service
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.instrumentation.NadelInstrumentationTimer
import graphql.nadel.engine.plan.NadelExecutionPlan
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField

class NadelQueryTransformer private constructor(
    private val service: Service,
    private val executionContext: NadelExecutionContext,
    private val executionPlan: NadelExecutionPlan,
    private val transformContext: TransformContext,
    private val timer: NadelInstrumentationTimer.BatchTimer,
) {
    companion object {
        suspend fun transformQuery(
            service: Service,
            executionContext: NadelExecutionContext,
            executionPlan: NadelExecutionPlan,
            field: ExecutableNormalizedField,
        ): TransformResult {
            val transformContext = TransformContext()

            executionContext.timer.batch().use { timer ->
                val transformer = NadelQueryTransformer(
                    service,
                    executionContext,
                    executionPlan,
                    transformContext,
                    timer,
                )
                val result = transformer.transform(field)
                    .also { rootFields ->
                        transformer.fixParentRefs(parent = null, rootFields)
                    }

                return TransformResult(
                    result = result,
                    artificialFields = transformContext.artificialFields,
                    overallToUnderlyingFields = transformContext.overallToUnderlyingFields,
                )
            }
        }
    }

    private data class TransformContext(
        val artificialFields: MutableList<ExecutableNormalizedField> = mutableListOf(),
        val overallToUnderlyingFields: MutableMap<ExecutableNormalizedField, MutableList<ExecutableNormalizedField>> = mutableMapOf(),
    )

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
        val transformationSteps: List<NadelExecutionPlan.Step> =
            executionPlan.transformationSteps[field]
                ?: return listOf(
                    transformPlain(field)
                )

        return transform(field, transformationSteps)
    }

    private suspend fun transform(
        field: ExecutableNormalizedField,
        transformationSteps: List<NadelExecutionPlan.Step>,
    ): List<ExecutableNormalizedField> {
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

        transformContext.artificialFields.addAll(artificialFields)

        // Track overall -> underlying fields
        transformContext.overallToUnderlyingFields
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
                transformContext.overallToUnderlyingFields
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
        transformationSteps: List<NadelExecutionPlan.Step>,
    ): NadelTransformFieldResult {
        var newField: ExecutableNormalizedField = field
        val artificialFields = mutableListOf<ExecutableNormalizedField>()

        for (transformStep in transformationSteps) {
            val transformResultForStep = timer.time(transformStep.queryTransformTimingStep) {
                transformStep.transform.transformField(
                    transformContext = transformStep.transformContext,
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
