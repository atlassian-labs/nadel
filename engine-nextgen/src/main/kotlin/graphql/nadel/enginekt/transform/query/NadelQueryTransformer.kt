package graphql.nadel.enginekt.transform.query

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelDeepRenameFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelRenameFieldInstruction
import graphql.nadel.enginekt.blueprint.getForField
import graphql.nadel.enginekt.transform.query.deepRename.NadelDeepRenameFieldTransform
import graphql.nadel.enginekt.util.copy
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

internal class NadelQueryTransformer(
    private val overallSchema: GraphQLSchema,
    private val executionBlueprint: NadelExecutionBlueprint,
    private val deepRenameTransform: NadelDeepRenameFieldTransform,
) {
    /**
     * Use this function.
     */
    fun transformQuery(
        service: Service,
        field: NormalizedField,
    ): List<NormalizedField> {
        return transformField(service, field).also { rootFields ->
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
    ): List<NormalizedField> {
        // TODO: refactor so this is based on execution plan rather than blueprint directly
        return when (val fieldInstruction = executionBlueprint.fieldInstructions.getForField(field)) {
            is NadelRenameFieldInstruction -> TODO()
            is NadelHydrationFieldInstruction -> TODO()
            is NadelBatchHydrationFieldInstruction -> TODO()
            is NadelDeepRenameFieldInstruction -> deepRenameTransform.transform(
                transformer = this,
                service,
                overallSchema,
                executionBlueprint,
                field,
                fieldInstruction,
            )
            null -> listOf(
                field.copy(
                    children = transformFields(service, field.children),
                ),
            )
        }
    }

    /**
     * API for transforms, do not use outside of transformer classes.
     *
     * @see [transformQuery]
     */
    internal fun transformFields(service: Service, fields: List<NormalizedField>): List<NormalizedField> {
        return fields.flatMap {
            transformField(service, it)
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
                deepRenameTransform = NadelDeepRenameFieldTransform(),
            )
        }
    }
}
