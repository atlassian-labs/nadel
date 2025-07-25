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
import graphql.nadel.engine.transform.NadelTransformServiceExecutionContext
import graphql.nadel.engine.transform.NadelTypeRenameResultTransform
import graphql.nadel.engine.transform.hydration.NadelHydrationTransform
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationTransform
import graphql.nadel.engine.transform.partition.NadelPartitionTransform
import graphql.nadel.engine.transform.skipInclude.NadelSkipIncludeTransform
import graphql.nadel.engine.transform.skipInclude.NadelSkipIncludeTransform.Companion.isSkipIncludeSpecialField
import graphql.nadel.engine.transform.stub.NadelStubTransform
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.ChildStep
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.ExecutionPlanning
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.QueryTransforming
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.ResultTransforming
import graphql.nadel.util.dfs
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
        service: Service,
        rootField: ExecutableNormalizedField,
        serviceHydrationDetails: ServiceExecutionHydrationDetails?,
    ): NadelExecutionPlan {
        val executionSteps: MutableMap<ExecutableNormalizedField, List<NadelExecutionPlan.Step<Any>>> =
            mutableMapOf()
        val transformContexts: MutableMap<NadelTransform<Any>, NadelTransformServiceExecutionContext?> =
            mutableMapOf()
        executionContext.timer.batch { timer ->
            traverseQuery(rootField) { field ->
                val steps = transformsWithTimingStepInfo.mapNotNull { transformWithTimingInfo ->
                    val transform = transformWithTimingInfo.transform
                    val executionTransformContext = transformContexts.getOrPut(transform) {
                        transform.buildContext(
                            executionContext,
                            serviceExecutionContext,
                            executionBlueprint,
                            services,
                            service,
                            rootField,
                            serviceHydrationDetails,
                        )
                    }
                    // This is a patch to prevent errors
                    // Ideally this should not happen but the proper fix requires more refactoring
                    // See NadelSkipIncludeTransform.isApplicable for more details
                    if (isSkipIncludeSpecialField(field) && ((transform as NadelTransform<*>) !is NadelSkipIncludeTransform)) {
                        null
                    } else {
                        val state = timer.time(step = transformWithTimingInfo.executionPlanTimingStep) {
                            transform.isApplicable(
                                executionContext,
                                serviceExecutionContext,
                                executionBlueprint,
                                services,
                                service,
                                field,
                                executionTransformContext,
                                serviceHydrationDetails,
                            )
                        }

                        if (state == null) {
                            null
                        } else {
                            NadelExecutionPlan.Step(
                                service = service,
                                field = field,
                                transform = transform,
                                queryTransformTimingStep = transformWithTimingInfo.queryTransformTimingStep,
                                resultTransformTimingStep = transformWithTimingInfo.resultTransformTimingStep,
                                state = state,
                                executionTransformContext
                            )
                        }
                    }
                }

                if (steps.isNotEmpty()) {
                    executionSteps[field] = steps
                }
            }
        }

        return NadelExecutionPlan(executionSteps)
    }

    private inline fun traverseQuery(
        root: ExecutableNormalizedField,
        consumer: (ExecutableNormalizedField) -> Unit,
    ) {
        dfs(
            root = root,
            getChildren = ExecutableNormalizedField::getChildren,
            consumer = consumer,
        )
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
                    NadelStubTransform(),
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
