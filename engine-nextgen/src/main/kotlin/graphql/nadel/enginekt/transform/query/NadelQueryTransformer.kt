package graphql.nadel.enginekt.transform.query

import graphql.nadel.Service
import graphql.nadel.enginekt.plan.NadelExecutionPlan
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

    suspend fun transformQuery(
        service: Service,
        field: NormalizedField,
        executionPlan: NadelExecutionPlan,
    ): List<NormalizedField> {
        return transformField(service, field, executionPlan).also { rootFields ->
            fixParentRefs(parent = null, rootFields)
        }
    }

    private suspend fun transformField(
        service: Service,
        field: NormalizedField,
        executionPlan: NadelExecutionPlan,
    ): List<NormalizedField> {
        val transformationSteps = executionPlan.transformationSteps[field] ?: return listOf(
            field.let {
                // Cannot inline this as transform lambda is not a coroutine
                val transformedChildFields = transformFields(service, it.children, executionPlan)
                it.transform { builder ->
                    builder.clearObjectTypesNames()
                    builder.objectTypeNames(field.objectTypeNames.map { overallTypeName ->
                        executionPlan.typeRenames[overallTypeName]?.underlyingName ?: overallTypeName
                    })
                    builder.children(transformedChildFields)
                }
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
                return transformFields(service, fields, executionPlan)
            }
        }
        val transformResult = transformation.transform.transformField(
            continuation,
            service,
            overallSchema,
            executionPlan,
            field,
            transformation.state,
        )

        val result = transformResult.extraFields.let { fields ->
            when (val newField = transformResult.newField) {
                null -> fields
                else -> fields + newField
            }
        }
        return patchObjectTypeNames(result, executionPlan)
    }

    private fun patchObjectTypeNames(
        fields: List<NormalizedField>,
        executionPlan: NadelExecutionPlan,
    ): List<NormalizedField> {
        return fields.map { field ->
            field.transform { builder ->
                builder.clearObjectTypesNames()
                builder.objectTypeNames(field.objectTypeNames.map {
                    executionPlan.typeRenames[it]?.underlyingName ?: it
                })
            }
        }
    }

    /**
     * Helper for calling [transformField] for all the given [fields].
     */
    private suspend fun transformFields(
        service: Service,
        fields: List<NormalizedField>,
        executionPlan: NadelExecutionPlan,
    ): List<NormalizedField> {
        return fields.flatMap {
            transformField(service, it, executionPlan)
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
            return NadelQueryTransformer(
                overallSchema,
            )
        }
    }
}
