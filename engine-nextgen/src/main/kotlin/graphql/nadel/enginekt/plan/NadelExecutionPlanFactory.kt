package graphql.nadel.enginekt.plan

import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.NadelCoerceTransform
import graphql.nadel.enginekt.transform.NadelDeepRenameTransform
import graphql.nadel.enginekt.transform.NadelRenameTransform
import graphql.nadel.enginekt.transform.NadelServiceTypeFilterTransform
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.NadelTypeRenameResultTransform
import graphql.nadel.enginekt.transform.hydration.NadelHydrationTransform
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationTransform
import graphql.nadel.enginekt.transform.skipInclude.SkipIncludeTransform
import graphql.normalized.ExecutableNormalizedField

internal class NadelExecutionPlanFactory(
    private val executionBlueprint: NadelOverallExecutionBlueprint,
    private val transforms: List<NadelTransform<Any>>,
) {
    /**
     * This derives an execution plan from with the main input parameters being the
     * [rootField] and [executionBlueprint].
     */
    suspend fun create(
        executionContext: NadelExecutionContext,
        services: Map<String, Service>,
        service: Service,
        rootField: ExecutableNormalizedField,
        serviceHydrationDetails: ServiceExecutionHydrationDetails? = null,
    ): NadelExecutionPlan {
        val executionSteps = mutableListOf<AnyNadelExecutionPlanStep>()

        traverseQuery(rootField) { field ->
            transforms.forEach { transform ->
                val state = transform.isApplicable(
                    executionContext,
                    executionBlueprint,
                    services,
                    service,
                    field,
                    serviceHydrationDetails,
                )
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
        )
    }

    private suspend fun traverseQuery(
        root: ExecutableNormalizedField,
        consumer: suspend (ExecutableNormalizedField) -> Unit,
    ) {
        consumer(root)
        root.children.forEach {
            traverseQuery(it, consumer)
        }
    }

    companion object {
        fun create(
            executionBlueprint: NadelOverallExecutionBlueprint,
            transforms: List<NadelTransform<out Any>>,
            engine: NextgenEngine,
        ): NadelExecutionPlanFactory {
            return NadelExecutionPlanFactory(
                executionBlueprint,
                transforms = listOfTransforms(
                    NadelCoerceTransform(),
                    SkipIncludeTransform(),
                    NadelServiceTypeFilterTransform(),
                    *transforms.toTypedArray(),
                    NadelDeepRenameTransform(),
                    NadelTypeRenameResultTransform(),
                    NadelHydrationTransform(engine),
                    NadelBatchHydrationTransform(engine),
                    NadelRenameTransform(),
                ),
            )
        }

        private fun listOfTransforms(vararg elements: NadelTransform<out Any>): List<NadelTransform<Any>> {
            return elements.map {
                @Suppress("UNCHECKED_CAST") // Ssh it's okay
                it as NadelTransform<Any>
            }
        }
    }
}
