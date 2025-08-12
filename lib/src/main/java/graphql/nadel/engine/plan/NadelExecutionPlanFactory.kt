package graphql.nadel.engine.plan

import graphql.nadel.NextgenEngine
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.transform.GenericNadelTransform
import graphql.nadel.engine.transform.NadelDeepRenameTransform
import graphql.nadel.engine.transform.NadelRenameArgumentInputTypesTransform
import graphql.nadel.engine.transform.NadelRenameTransform
import graphql.nadel.engine.transform.NadelServiceTypeFilterTransform
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformOperationContext
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
    transforms: List<GenericNadelTransform>,
) {
    /**
     * This creates the [ChildStep] objects upfront to avoid constantly recreating them.
     */
    private data class TransformWithTimingInfo(
        val transform: GenericNadelTransform,
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
        operationExecutionContext: NadelOperationExecutionContext,
        rootField: ExecutableNormalizedField,
    ): NadelExecutionPlan {
        val executionSteps: MutableMap<ExecutableNormalizedField, List<NadelExecutionPlan.Step>> =
            mutableMapOf()
        val transformContexts: MutableMap<GenericNadelTransform, NadelTransformOperationContext> =
            mutableMapOf()
        executionContext.timer.batch { timer ->
            traverseQuery(rootField) { field ->
                val steps = transformsWithTimingStepInfo.mapNotNull { transformWithTimingInfo ->
                    val transform = transformWithTimingInfo.transform
                    // This is a patch to prevent errors
                    // Ideally this should not happen but the proper fix requires more refactoring
                    // See NadelSkipIncludeTransform.isApplicable for more details
                    if (isSkipIncludeSpecialField(field) && ((transform as NadelTransform<*, *>) !is NadelSkipIncludeTransform)) {
                        null
                    } else {
                        val transformOperationContext = transformContexts.getOrPut(transform) {
                            transform.getTransformOperationContext(operationExecutionContext)
                        }

                        val state = timer.time(step = transformWithTimingInfo.executionPlanTimingStep) {
                            transform.getTransformFieldContext(
                                transformOperationContext,
                                field,
                            )
                        }

                        if (state == null) {
                            null
                        } else {
                            NadelExecutionPlan.Step(
                                transform = transform,
                                transformContext = state,
                                queryTransformTimingStep = transformWithTimingInfo.queryTransformTimingStep,
                                resultTransformTimingStep = transformWithTimingInfo.resultTransformTimingStep,
                            )
                        }
                    }
                }

                if (steps.isNotEmpty()) {
                    executionSteps[field] = steps
                }
            }
        }

        return NadelExecutionPlan(
            executionSteps,
            transformContexts,
        )
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
            engine: NextgenEngine,
            transforms: List<NadelTransform<*, *>>,
            executionHooks: NadelExecutionHooks,
        ): NadelExecutionPlanFactory {
            return NadelExecutionPlanFactory(
                transforms = transforms(
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

        private fun transforms(vararg elements: NadelTransform<*, *>): List<GenericNadelTransform> {
            return elements.map {
                @Suppress("UNCHECKED_CAST") // Ssh it's okay
                it as GenericNadelTransform
            }
        }
    }
}
