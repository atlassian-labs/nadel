package graphql.nadel.enginekt.transform.query

import graphql.nadel.Service
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.NadelTransformFieldResult
import graphql.nadel.enginekt.util.toBuilder
import graphql.normalized.ExecutableNormalizedField

class NadelQueryTransformer internal constructor(
    private val executionBlueprint: NadelOverallExecutionBlueprint,
) {
    interface Continuation {
        suspend fun transform(fields: ExecutableNormalizedField): List<ExecutableNormalizedField> {
            return transform(listOf(fields))
        }

        suspend fun transform(fields: List<ExecutableNormalizedField>): List<ExecutableNormalizedField>
    }

    data class TransformContext(
        val artificialFields: MutableList<ExecutableNormalizedField>,
        val overallToUnderlyingFields: MutableMap<ExecutableNormalizedField, List<ExecutableNormalizedField>>,
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

    suspend fun transformQuery(
        executionContext: NadelExecutionContext,
        service: Service,
        field: ExecutableNormalizedField,
        executionPlan: NadelExecutionPlan,
    ): TransformResult {
        val transformContext = TransformContext(
            artificialFields = mutableListOf(),
            overallToUnderlyingFields = mutableMapOf(),
        )

        val result = transformField(executionContext, transformContext, executionPlan, service, field)
            .also { rootFields ->
                fixParentRefs(parent = null, rootFields)
            }

        return TransformResult(
            result = result,
            artificialFields = transformContext.artificialFields,
            overallToUnderlyingFields = transformContext.overallToUnderlyingFields,
        )
    }

    private suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformContext: TransformContext,
        executionPlan: NadelExecutionPlan,
        service: Service,
        field: ExecutableNormalizedField,
    ): List<ExecutableNormalizedField> {
        val transformationSteps: List<NadelExecutionPlan.Step<Any>> = executionPlan.transformationSteps[field] ?: return listOf(
            field.let {
                val transformedChildFields = transformFields(
                    executionContext = executionContext,
                    transformContext = transformContext,
                    service = service,
                    fields = it.children,
                    executionPlan = executionPlan
                )
                patchObjectTypeNames(field)
                    .children(transformedChildFields)
                    .build()
                    .also { newField ->
                        // Track overall -> underlying fields
                        transformContext.overallToUnderlyingFields.compute(field) { _, oldValue ->
                            (oldValue ?: emptyList()) + newField
                        }
                    }
            }
        )
        val continuation = object : Continuation {
            override suspend fun transform(fields: List<ExecutableNormalizedField>): List<ExecutableNormalizedField> {
                return transformFields(
                    executionContext = executionContext,
                    transformContext = transformContext,
                    service = service,
                    fields = fields,
                    executionPlan = executionPlan,
                )
            }
        }

        val transformResult = applyTransformationSteps(field, transformationSteps, executionContext, continuation, service)

        // TODO: I think that patching names here is wrong, we're giving mixed signals to the transformations
        // i.e. yes please give us a field in the underlying sense, but only field name
        // I think this introduces confusion
        val patchedArtificialFields = patchObjectTypeNames(transformResult.artificialFields)
        val patchedNewField = patchObjectTypeNames(listOfNotNull(transformResult.newField))
        transformContext.artificialFields.addAll(patchedArtificialFields)

        // Track overall -> underlying fields
        transformContext.overallToUnderlyingFields.compute(field) { _, oldValue ->
            (oldValue ?: emptyList()) + patchedNewField + patchedArtificialFields
        }

        return patchedArtificialFields + patchedNewField
    }

    private suspend fun applyTransformationSteps(
        field: ExecutableNormalizedField,
        transformationSteps: List<NadelExecutionPlan.Step<Any>>,
        executionContext: NadelExecutionContext,
        continuation: Continuation,
        service: Service
    ): NadelTransformFieldResult {
        var fieldFromPreviousTransform: ExecutableNormalizedField = field
        var transformFieldResult: NadelTransformFieldResult? = null
        for ((_, _, transform, state) in transformationSteps) {
            transformFieldResult = transform.transformField(
                executionContext,
                continuation,
                executionBlueprint,
                service,
                fieldFromPreviousTransform,
                state,
            )
            fieldFromPreviousTransform = transformFieldResult.newField ?: break
        }
        return transformFieldResult!!
    }

    private fun patchObjectTypeNames(
        fields: List<ExecutableNormalizedField>,
    ): List<ExecutableNormalizedField> {
        return fields.map { field ->
            patchObjectTypeNames(field).build()
        }
    }

    private fun patchObjectTypeNames(
        field: ExecutableNormalizedField,
    ): ExecutableNormalizedField.Builder {
        return field.toBuilder()
            .clearObjectTypesNames()
            .objectTypeNames(field.objectTypeNames.map(executionBlueprint::getUnderlyingTypeName))
    }

    /**
     * Helper for calling [transformField] for all the given [fields].
     */
    private suspend fun transformFields(
        executionContext: NadelExecutionContext,
        transformContext: TransformContext,
        service: Service,
        fields: List<ExecutableNormalizedField>,
        executionPlan: NadelExecutionPlan,
    ): List<ExecutableNormalizedField> {
        return fields.flatMap {
            transformField(executionContext, transformContext, executionPlan, service, it)
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

    companion object {
        fun create(
            executionBlueprint: NadelOverallExecutionBlueprint,
        ): NadelQueryTransformer {
            return NadelQueryTransformer(executionBlueprint)
        }
    }
}
