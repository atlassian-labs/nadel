package graphql.nadel.engine.transform.query

import graphql.nadel.Service
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.plan.NadelExecutionPlan
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField

class NadelQueryTransformer private constructor(
    private val executionBlueprint: NadelOverallExecutionBlueprint,
    private val service: Service,
    private val executionContext: NadelExecutionContext,
    private val executionPlan: NadelExecutionPlan,
    private val transformContext: TransformContext,
) {
    companion object {
        suspend fun transformQuery(
            executionBlueprint: NadelOverallExecutionBlueprint,
            service: Service,
            executionContext: NadelExecutionContext,
            executionPlan: NadelExecutionPlan,
            field: ExecutableNormalizedField,
        ): TransformResult {
            val transformContext = TransformContext()

            val transformer = NadelQueryTransformer(
                executionBlueprint,
                service,
                executionContext,
                executionPlan,
                transformContext,
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

    private data class TransformContext(
        val artificialFields: MutableList<ExecutableNormalizedField> = mutableListOf(),
        val overallToUnderlyingFields: MutableMap<ExecutableNormalizedField, List<ExecutableNormalizedField>> = mutableMapOf(),
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

    fun markArtificial(field: ExecutableNormalizedField) {
        transformContext.artificialFields.add(field)
    }

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
        val transformationSteps: List<NadelExecutionPlan.Step<Any>> = executionPlan.transformationSteps[field]
            ?: return listOf(
                transformPlain(field)
            )

        return transform(field, transformationSteps)
    }

    private suspend fun transform(
        field: ExecutableNormalizedField,
        transformationSteps: List<NadelExecutionPlan.Step<Any>>,
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
        transformContext.overallToUnderlyingFields.compute(field) { _, oldValue ->
            (oldValue ?: emptyList()) + newField + artificialFields
        }

        return artificialFields + newField
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
                transformContext.overallToUnderlyingFields.compute(field) { _, oldValue ->
                    (oldValue ?: emptyList()) + newField
                }
            }
    }

    private suspend fun applyTransformationSteps(
        field: ExecutableNormalizedField,
        transformationSteps: List<NadelExecutionPlan.Step<Any>>,
    ): NadelTransformFieldResult {
        var fieldFromPreviousTransform: ExecutableNormalizedField = field
        var aggregatedTransformResult: NadelTransformFieldResult? = null
        for ((_, _, transform, state) in transformationSteps) {
            val transformResultForStep = transform.transformField(
                executionContext,
                this,
                executionBlueprint,
                service,
                fieldFromPreviousTransform,
                state,
            )
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
        parent: ExecutableNormalizedField?,
        transformFields: List<ExecutableNormalizedField>,
    ) {
        transformFields.forEach {
            it.replaceParent(parent)
            fixParentRefs(parent = it, it.children)
        }
    }
}
