package graphql.nadel.engine.transform.query

import graphql.nadel.Service
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.instrumentation.NadelInstrumentationTimer
import graphql.nadel.engine.plan.NadelExecutionPlan
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.util.toBuilder
import graphql.nadel.engine.compiler.NadelExecutableNormalizedField

class NadelQueryTransformer private constructor(
    private val executionBlueprint: NadelOverallExecutionBlueprint,
    private val service: Service,
    private val executionContext: NadelExecutionContext,
    private val serviceExecutionContext: NadelServiceExecutionContext,
    private val executionPlan: NadelExecutionPlan,
    private val transformContext: TransformContext,
    private val timer: NadelInstrumentationTimer.BatchTimer,
) {
    companion object {
        suspend fun transformQuery(
            executionBlueprint: NadelOverallExecutionBlueprint,
            service: Service,
            executionContext: NadelExecutionContext,
            serviceExecutionContext: NadelServiceExecutionContext,
            executionPlan: NadelExecutionPlan,
            field: NadelExecutableNormalizedField,
        ): TransformResult {
            val transformContext = TransformContext()

            executionContext.timer.batch().use { timer ->
                val transformer = NadelQueryTransformer(
                    executionBlueprint,
                    service,
                    executionContext,
                    serviceExecutionContext,
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
        val artificialFields: MutableList<NadelExecutableNormalizedField> = mutableListOf(),
        val overallToUnderlyingFields: MutableMap<NadelExecutableNormalizedField, MutableList<NadelExecutableNormalizedField>> = mutableMapOf(),
    )

    data class TransformResult(
        /**
         * The transformed fields.
         */
        val result: List<NadelExecutableNormalizedField>,
        /**
         * A list of fields that were added to the query that do not belong in the overall result.
         */
        val artificialFields: List<NadelExecutableNormalizedField>,
        val overallToUnderlyingFields: Map<NadelExecutableNormalizedField, List<NadelExecutableNormalizedField>>,
    )

    /**
     * Helper for calling [transform] for all the given [fields].
     */
    suspend fun transform(
        fields: List<NadelExecutableNormalizedField>,
    ): List<NadelExecutableNormalizedField> {
        return fields.flatMap {
            transform(it)
        }
    }

    suspend fun transform(
        field: NadelExecutableNormalizedField,
    ): List<NadelExecutableNormalizedField> {
        val transformationSteps: List<NadelExecutionPlan.Step<Any>> =
            executionPlan.transformationSteps[field]
                ?: return listOf(
                    transformPlain(field)
                )

        return transform(field, transformationSteps)
    }

    private suspend fun transform(
        field: NadelExecutableNormalizedField,
        transformationSteps: List<NadelExecutionPlan.Step<Any>>,
    ): List<NadelExecutableNormalizedField> {
        val transformResult = applyTransformationSteps(field, transformationSteps)

        // A transform sets forcePrintAsUnconditional directly on the fields it returns; toBuilder() copies the
        // flag, so it survives this rebuild onto the exact instances that flow to the forked compiler. No
        // external signal is threaded through the engine.
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
    private suspend fun transformPlain(field: NadelExecutableNormalizedField): NadelExecutableNormalizedField {
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
        field: NadelExecutableNormalizedField,
        transformationSteps: List<NadelExecutionPlan.Step<Any>>,
    ): NadelTransformFieldResult {
        var newField: NadelExecutableNormalizedField = field
        val artificialFields = mutableListOf<NadelExecutableNormalizedField>()

        for (transformStep in transformationSteps) {
            val transformServiceExecutionContext = executionPlan.transformContexts[transformStep.transform]
            val transformResultForStep = timer.time(transformStep.queryTransformTimingStep) {
                transformStep.transform.transformField(
                    executionContext,
                    serviceExecutionContext,
                    this,
                    executionBlueprint,
                    service,
                    newField,
                    transformStep.state,
                    transformServiceExecutionContext
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
                executionBlueprint.getUnderlyingTypeName(overallTypeName = it)
            }
        } else {
            objectTypeNames.map {
                executionBlueprint.getUnderlyingTypeName(service, overallTypeName = it)
            }
        }
    }

    private fun fixParentRefs(
        parent: NadelExecutableNormalizedField?,
        transformFields: List<NadelExecutableNormalizedField>,
    ) {
        transformFields.forEach {
            it.replaceParent(parent)
            fixParentRefs(parent = it, it.children)
        }
    }
}
