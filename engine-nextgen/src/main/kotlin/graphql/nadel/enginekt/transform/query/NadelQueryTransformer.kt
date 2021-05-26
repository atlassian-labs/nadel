package graphql.nadel.enginekt.transform.query

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.deepRename.NadelDeepRenameTransform
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

fun interface NadelQueryTransformerContinue {
    fun transformNext(field: List<NormalizedField>): List<NormalizedField>
}

internal class NadelQueryTransformer(
    private val overallSchema: GraphQLSchema,
    private val executionBlueprint: NadelExecutionBlueprint,
    private val deepRenameTransform: NadelDeepRenameTransform,
) {
    /**
     * Use this function.
     */
    fun transformQuery(
        service: Service,
        field: NormalizedField,
        executionPlan: NadelExecutionPlan,
    ): List<NormalizedField> {
        return transformField(service, field, executionPlan).also { rootFields ->
            fixParentRefs(parent = null, rootFields)
        }
    }

    /**
     * API for transforms, do not use outside of transformer classes.
     *
     * @see [transformQuery]
     */
    internal fun transformField(
        service: Service,
        field: NormalizedField,
        executionPlan: NadelExecutionPlan,
    ): List<NormalizedField> {
        val transformations = executionPlan.transformations[field] ?: return listOf(
            field.transform {
                it.children(transformFields(service, field.children, executionPlan))
            }
        )

        /**
         * transformField(): List<>
         * transformField(): FieldTransformResult
         *
         *     IDEA: to work on multiple transformations we can have this structure and then pass in the originalField to the subsequent transforms
         *     data class FieldTransformResult(
         *          val originalField: NF,
         *          val extraFields: NF,
         *     )
         */
        val transformation = transformations.single()
        return transformation.transform.transformField(
            {
                transformFields(service, it, executionPlan)
            },
            service,
            overallSchema,
            executionBlueprint,
            field,
            transformation.state,
        )
        // return when (val fieldInstruction = executionBlueprint.fieldInstructions.getForField(field)) {
        //     is NadelRenameFieldInstruction -> TODO()
        //     is NadelHydrationFieldInstruction -> TODO()
        //     is NadelBatchHydrationFieldInstruction -> TODO()
        //     is NadelDeepRenameFieldInstruction -> deepRenameTransform.transformField(
        //         transformer = this,
        //         service,
        //         overallSchema,
        //         executionBlueprint,
        //         field,
        //         fieldInstruction,
        //     )
        //     null -> listOf(
        //         field.transform {
        //             it.children(transformFields(service, field.children))
        //         }
        //     )
        // }
    }

    /**
     * API for transforms, do not use outside of transformer classes.
     *
     * @see [transformQuery]
     */
    internal fun transformFields(
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
        fun create(overallSchema: GraphQLSchema, executionBlueprint: NadelExecutionBlueprint): NadelQueryTransformer {
            return NadelQueryTransformer(
                overallSchema,
                executionBlueprint,
                deepRenameTransform = NadelDeepRenameTransform(),
            )
        }
    }
}
