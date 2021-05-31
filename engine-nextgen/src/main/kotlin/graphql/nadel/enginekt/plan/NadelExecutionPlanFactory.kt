package graphql.nadel.enginekt.plan

import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.blueprint.NadelTypeRenameInstruction
import graphql.nadel.enginekt.transform.NadelDeepRenameTransform
import graphql.nadel.enginekt.transform.NadelTypeRenameResultTransform
import graphql.nadel.enginekt.transform.hydration.NadelHydrationTransform
import graphql.nadel.enginekt.transform.query.AnyNadelTransform
import graphql.nadel.enginekt.transform.query.NadelTransform
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

internal class NadelExecutionPlanFactory(
    private val executionBlueprint: NadelExecutionBlueprint,
    private val overallSchema: GraphQLSchema,
    private val transforms: List<AnyNadelTransform>,
) {
    /**
     * This derives an execution plan from with the main input parameters being the
     * [rootField] and [executionBlueprint].
     */
    suspend fun create(
        userContext: Any?,
        service: Service,
        rootField: NormalizedField,
    ): NadelExecutionPlan {
        val executionSteps = mutableListOf<AnyNadelExecutionPlanStep>()
        val relevantTypeRenames = mutableMapOf<String, NadelTypeRenameInstruction>()

        traverseQuery(rootField) { field ->
            field.objectTypeNames.mapNotNull { executionBlueprint.typeInstructions[it] }
                .forEach { relevantTypeRenames[it.overallName] = it }

            transforms.forEach { transform ->
                val state = transform.isApplicable(userContext, overallSchema, executionBlueprint, service, field)
                if (state != null) {
                    executionSteps.add(
                        NadelExecutionPlan.Step(
                            service,
                            field,
                            transform,
                            state,
                        ),
                    )
                }
            }
        }

        return NadelExecutionPlan(
            executionSteps.groupBy { it.field },
            relevantTypeRenames
        )
    }

    private suspend fun traverseQuery(root: NormalizedField, consumer: suspend (NormalizedField) -> Unit) {
        consumer(root)
        root.children.forEach {
            traverseQuery(it, consumer)
        }
    }

    companion object {
        fun create(
            executionBlueprint: NadelExecutionBlueprint,
            overallSchema: GraphQLSchema,
            engine: NextgenEngine,
        ): NadelExecutionPlanFactory {
            return NadelExecutionPlanFactory(
                executionBlueprint,
                overallSchema,
                transforms = listOfTransforms(
                    NadelDeepRenameTransform(),
                    NadelTypeRenameResultTransform(),
                    NadelHydrationTransform(engine),
                ),
            )
        }

        private fun listOfTransforms(vararg elements: NadelTransform<out Any>): List<AnyNadelTransform> {
            return elements.map {
                @Suppress("UNCHECKED_CAST") // Ssh it's okay
                it as AnyNadelTransform
            }
        }
    }
}
