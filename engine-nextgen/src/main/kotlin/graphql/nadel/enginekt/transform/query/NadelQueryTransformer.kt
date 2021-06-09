package graphql.nadel.enginekt.transform.query

import graphql.nadel.Service
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.NadelTransformFieldResult
import graphql.nadel.enginekt.util.toBuilder
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

class NadelQueryTransformer internal constructor(
    private val overallSchema: GraphQLSchema,
) {
    interface Continuation {
        suspend fun transform(fields: NormalizedField): List<NormalizedField> {
            return transform(listOf(fields))
        }

        suspend fun transform(fields: List<NormalizedField>): List<NormalizedField>
    }

    internal data class TransformContext(
        val artificialFields: MutableList<NormalizedField>,
        val overallToUnderlyingFields: MutableMap<NormalizedField, List<NormalizedField>>,
    )

    data class TransformResult(
        /**
         * The transformed fields.
         */
        val result: List<NormalizedField>,
        /**
         * A list of fields that were added to the query that do not belong in the overall result.
         */
        val artificialFields: List<NormalizedField>,
        val overallToUnderlyingFields: Map<NormalizedField, List<NormalizedField>>,
    )

    suspend fun transformQuery(
        executionContext: NadelExecutionContext,
        service: Service,
        field: NormalizedField,
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
        field: NormalizedField,
    ): List<NormalizedField> {
        val transformationSteps = executionPlan.transformationSteps[field] ?: return listOf(
            field.let {
                val transformedChildFields = transformFields(
                    executionContext = executionContext,
                    transformContext = transformContext,
                    service = service,
                    fields = it.children,
                    executionPlan = executionPlan
                )
                patchObjectTypeNames(field, executionPlan)
                    .children(transformedChildFields)
                    .build()
            }
        )

        /**
         * TODO: determine how to handle multiple transformation steps e.g.
         *
         * issueByARI(ari: ID @ARI): Issue @renamed(from: "issueById")
         *
         * Will have two transformation steps on it. The ARI transform and the rename transform.
         *
         * Ideally we need to just pass on [NadelTransformFieldResult.newField] to the next transformer.
         *
         * BUT, what happens when one transform sets [NadelTransformFieldResult.newField] to null to remove
         * the field? In that case the other transforms may be left in a unstable state as they might
         * still be expecting to be executed.
         */
        val transformation = transformationSteps.single()
        val continuation = object : Continuation {
            override suspend fun transform(fields: List<NormalizedField>): List<NormalizedField> {
                return transformFields(
                    executionContext = executionContext,
                    transformContext = transformContext,
                    service = service,
                    fields = fields,
                    executionPlan = executionPlan,
                )
            }
        }

        val transformResult = transformation.transform.transformField(
            executionContext,
            continuation,
            service,
            overallSchema,
            executionPlan,
            field,
            transformation.state,
        )

        // TODO: I think that patching names here is wrong, we're giving mixed signals to the transformations
        // i.e. yes please give us a field in the underlying sense, but only field name
        // I think this introduces confusion
        val patchedArtificialFields = patchObjectTypeNames(transformResult.artificialFields, executionPlan)
        val patchedNewField = patchObjectTypeNames(listOfNotNull(transformResult.newField), executionPlan)
        transformContext.artificialFields.addAll(patchedArtificialFields)

        // Track overall -> underlying fields
        transformContext.overallToUnderlyingFields.compute(field) { _, oldValue ->
            (oldValue ?: emptyList()) + patchedNewField + patchedArtificialFields
        }

        return patchedArtificialFields + patchedNewField
    }

    private fun patchObjectTypeNames(
        fields: List<NormalizedField>,
        executionPlan: NadelExecutionPlan,
    ): List<NormalizedField> {
        return fields.map { field ->
            patchObjectTypeNames(field, executionPlan).build()
        }
    }

    private fun patchObjectTypeNames(
        field: NormalizedField,
        executionPlan: NadelExecutionPlan,
    ): NormalizedField.Builder {
        return field.toBuilder()
            .clearObjectTypesNames()
            .objectTypeNames(field.objectTypeNames.map(executionPlan::getUnderlyingTypeName))
    }

    /**
     * Helper for calling [transformField] for all the given [fields].
     */
    private suspend fun transformFields(
        executionContext: NadelExecutionContext,
        transformContext: TransformContext,
        service: Service,
        fields: List<NormalizedField>,
        executionPlan: NadelExecutionPlan,
    ): List<NormalizedField> {
        return fields.flatMap {
            transformField(executionContext, transformContext, executionPlan, service, it)
        }
    }

    private fun fixParentRefs(
        parent: NormalizedField?,
        transformFields: List<NormalizedField>,
    ) {
        transformFields.forEach {
            it.replaceParent(parent)
            fixParentRefs(parent = it, it.children)
        }
    }

    companion object {
        fun create(overallSchema: GraphQLSchema): NadelQueryTransformer {
            return NadelQueryTransformer(overallSchema)
        }
    }
}
