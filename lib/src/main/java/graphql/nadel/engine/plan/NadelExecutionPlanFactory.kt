package graphql.nadel.engine.plan

import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceLike
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
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.QueryTransforming
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.ResultTransforming
import graphql.normalized.ExecutableNormalizedField

internal class NadelExecutionPlanFactory(
    private val executionBlueprint: NadelOverallExecutionBlueprint,
    transforms: List<NadelTransform<Any>>,
) {
    /**
     * This creates the [ChildStep] objects upfront to avoid constantly recreating them.
     */
    private data class TransformWithTimingInfo(
        val transform: NadelTransform<Any>,
        val executionPlanTimingStep: ChildStep,
        val queryTransformTimingStep: ChildStep,
        val resultTransformTimingStep: ChildStep,
    )

    private val transformsWithTimingStepInfo = transforms
        .map { transform ->
            TransformWithTimingInfo(
                transform = transform,
                executionPlanTimingStep = ChildStep(parent = ExecutionPlanning, transform = transform),
                queryTransformTimingStep = ChildStep(parent = QueryTransforming, transform = transform),
                resultTransformTimingStep = ChildStep(parent = ResultTransforming, transform = transform),
            )
        }

    /**
     * This derives an execution plan from with the main input parameters being the
     * [rootField] and [executionBlueprint].
     */
    suspend fun create(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        services: Map<String, Service>,
        service: ServiceLike,
        rootField: ExecutableNormalizedField,
        serviceHydrationDetails: ServiceExecutionHydrationDetails?,
    ): NadelExecutionPlan {
        val executionSteps = mutableListOf<AnyNadelExecutionPlanStep>()

        executionContext.timer.batch { timer ->
            traverseQuery(rootField) { field ->
                transformsWithTimingStepInfo.forEach { transformWithTimingInfo ->
                    val transform = transformWithTimingInfo.transform
                    // This is a patch to prevent errors
                    // Ideally this should not happen but the proper fix requires more refactoring
                    // See NadelSkipIncludeTransform.isApplicable for more details
                    if (isSkipIncludeSpecialField(field) && ((transform as NadelTransform<*>) !is NadelSkipIncludeTransform)) {
                        return@forEach
                    }

                    val state = timer.time(step = transformWithTimingInfo.executionPlanTimingStep) {
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
                                service = service,
                                field = field,
                                transform = transform,
                                queryTransformTimingStep = transformWithTimingInfo.queryTransformTimingStep,
                                resultTransformTimingStep = transformWithTimingInfo.resultTransformTimingStep,
                                state = state,
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
