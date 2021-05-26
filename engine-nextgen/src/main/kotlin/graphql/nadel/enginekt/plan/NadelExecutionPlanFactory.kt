package graphql.nadel.enginekt.plan

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.transform.deepRename.NadelDeepRenameTransform
import graphql.nadel.enginekt.transform.query.NadelTransform
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

internal class NadelExecutionPlanFactory(
    private val executionBlueprint: NadelExecutionBlueprint,
    private val overallSchema: GraphQLSchema,
    private val transforms: List<NadelTransform<*>>,
) {

    /**
     * This derives an execution plan from with the main input parameters being the
     * [rootField] and [executionBlueprint].
     */
    fun create(
        userContext: Any?,
        service: Service,
        rootField: NormalizedField,
    ): NadelExecutionPlan {
        val steps = mutableListOf<NadelExecutionPlan.Step>()

        traverseQuery(rootField) { field ->
            transforms.forEach { transform ->
                val state = transform.isApplicable(userContext, overallSchema, executionBlueprint, service, field)
                if (state != null) {
                    steps.add(NadelExecutionPlan.Step(
                        service,
                        field,
                        transform as NadelTransform<Any>,
                        state,
                    ))
                }
            }
        }

        return NadelExecutionPlan(
            steps.groupBy { it.field },
        )
    }

    private fun traverseQuery(root: NormalizedField, consumer: (NormalizedField) -> Unit) {
        consumer(root)
        root.children.forEach {
            traverseQuery(it, consumer)
        }
    }

    companion object {
        fun create(
            executionBlueprint: NadelExecutionBlueprint,
            overallSchema: GraphQLSchema,
        ): NadelExecutionPlanFactory {
            return NadelExecutionPlanFactory(
                executionBlueprint,
                overallSchema,
                transforms = listOf(
                    NadelDeepRenameTransform(),
                ),
            )
        }
    }
}
