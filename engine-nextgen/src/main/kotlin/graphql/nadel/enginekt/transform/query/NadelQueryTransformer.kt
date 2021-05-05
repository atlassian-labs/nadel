package graphql.nadel.enginekt.transform.query

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelDeepRenameFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelRenameFieldInstruction
import graphql.nadel.enginekt.blueprint.getForField
import graphql.nadel.enginekt.transform.query.deepRename.NadelDeepRenameQueryTransform
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

internal class NadelQueryTransformer(
    private val overallSchema: GraphQLSchema,
    private val executionBlueprint: NadelExecutionBlueprint,
    private val deepRenameTransform: NadelDeepRenameQueryTransform,
) {
    // TODO: refactor so this is based on execution plan rather than blueprint directly
    fun transform(
        service: Service,
        field: NormalizedField,
    ): List<NormalizedField> {
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
                field.transform {
                    it.children(
                        field.children.flatMap { child ->
                            transform(service, field = child)
                        }
                    )
                }
            )
        }
    }

    companion object {
        fun create(overallSchema: GraphQLSchema, executionBlueprint: NadelExecutionBlueprint): NadelQueryTransformer {
            return NadelQueryTransformer(
                overallSchema,
                executionBlueprint,
                deepRenameTransform = NadelDeepRenameQueryTransform(),
            )
        }
    }
}
