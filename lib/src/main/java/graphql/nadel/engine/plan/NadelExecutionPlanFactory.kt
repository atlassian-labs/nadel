package graphql.nadel.engine.plan

import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelDeepRenameTransform
import graphql.nadel.engine.transform.NadelRenameArgumentInputTypesTransform
import graphql.nadel.engine.transform.NadelRenameTransform
import graphql.nadel.engine.transform.NadelServiceTypeFilterTransform
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTypeRenameResultTransform
import graphql.nadel.engine.transform.hydration.NadelHydrationTransform
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationTransform
import graphql.nadel.engine.transform.partition.NadelPartitionTransform
import graphql.nadel.engine.transform.skipInclude.NadelSkipIncludeTransform
import graphql.nadel.engine.transform.skipInclude.NadelSkipIncludeTransform.Companion.isSkipIncludeSpecialField
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.ChildStep
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.ExecutionPlanning
import graphql.normalized.ExecutableNormalizedField

internal class NadelExecutionPlanFactory(
    private val executionBlueprint: NadelOverallExecutionBlueprint,
    private val transforms: List<NadelTransform<Any>>,
) {
    // This will avoid creating the ChildStep object too many times
    private val transformsWithTimingStepInfo = transforms
        .map { transform ->
            transform to ChildStep(parent = ExecutionPlanning, transform = transform)
        }

    /**
     * This derives an execution plan from with the main input parameters being the
     * [rootField] and [executionBlueprint].
     */
    suspend fun create(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        services: Map<String, Service>,
        service: Service,
        rootField: ExecutableNormalizedField,
        serviceHydrationDetails: ServiceExecutionHydrationDetails?,
    ): NadelExecutionPlan {
        val executionSteps = mutableListOf<AnyNadelExecutionPlanStep>()

        executionContext.timer.batch { timer ->
            traverseQuery(rootField) { field ->
                transformsWithTimingStepInfo.forEach { (transform, timingStep) ->
                    // This is a patch to prevent errors
                    // Ideally this should not happen but the proper fix requires more refactoring
                    // See NadelSkipIncludeTransform.isApplicable for more details
                    if (isSkipIncludeSpecialField(field) && ((transform as NadelTransform<*>) !is NadelSkipIncludeTransform)) {
                        return@forEach
                    }

                    val state = timer.time(step = timingStep) {
                        transform.isApplicable(
                            executionContext,
                            serviceExecutionContext,
                            executionBlueprint,
                            services,
                            service,
                            field,
                            serviceHydrationDetails,
                        )
                    }

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
            executionHooks: NadelExecutionHooks,
        ): NadelExecutionPlanFactory {
            return NadelExecutionPlanFactory(
                executionBlueprint,
                transforms = listOfTransforms(
                    NadelSkipIncludeTransform(),
                    NadelServiceTypeFilterTransform(),
                    NadelPartitionTransform(engine, executionHooks.partitionTransformerHook()),
                    *transforms.toTypedArray(),
                    // *executionHooks.transformersWithQueryExecutor(QueryExecutor(engine::executeTopLevelField)).toTypedArray(),
                    NadelDeepRenameTransform(),
                    NadelTypeRenameResultTransform(),
                    NadelHydrationTransform(engine),
                    NadelBatchHydrationTransform(engine),
                    NadelRenameArgumentInputTypesTransform(),
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
