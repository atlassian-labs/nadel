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
import graphql.nadel.engine.transform.NadelTransformTimingSteps
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
    private data class Transform(
        val transform: GenericNadelTransform,
        val timingSteps: NadelTransformTimingSteps,
    )

    private val transforms = transforms
        .map { transform ->
            Transform(
                transform = transform,
                timingSteps = NadelTransformTimingSteps(
                    executionPlan = ChildStep(parent = ExecutionPlanning, transform = transform),
                    queryTransform = ChildStep(parent = QueryTransforming, transform = transform),
                    resultTransform = ChildStep(parent = ResultTransforming, transform = transform),
                ),
            )
        }

    /**
     * This derives an execution plan from with the main input parameters being the
     * [rootField] and [executionBlueprint].
     */
    suspend fun create(
        operationExecutionContext: NadelOperationExecutionContext,
        rootField: ExecutableNormalizedField,
    ): NadelExecutionPlan {
        val transformOperationContexts = transforms.associate { (transform) ->
            transform to transform.getTransformOperationContext(operationExecutionContext)
        }

        val executionSteps: MutableMap<ExecutableNormalizedField, List<NadelExecutionPlan.TransformFieldStep>> =
            mutableMapOf()
        operationExecutionContext.timer.batch { timer ->
            traverseQuery(rootField) { field ->
                val transformFieldSteps = transforms.mapNotNull { (transform, timingSteps) ->
                    // This is a patch to prevent errors
                    // Ideally this should not happen but the proper fix requires more refactoring
                    // See NadelSkipIncludeTransform.getTransformFieldContext for more details
                    if (isSkipIncludeSpecialField(field) && ((transform as NadelTransform<*, *>) !is NadelSkipIncludeTransform)) {
                        null
                    } else {
                        val transformFieldContext = timer.time(step = timingSteps.executionPlan) {
                            transform.getTransformFieldContext(
                                transformOperationContexts[transform]!!,
                                field,
                            )
                        }

                        if (transformFieldContext == null) {
                            null
                        } else {
                            NadelExecutionPlan.TransformFieldStep(
                                transform = transform,
                                transformFieldContext = transformFieldContext,
                                timingSteps = timingSteps,
                            )
                        }
                    }
                }

                if (transformFieldSteps.isNotEmpty()) {
                    executionSteps[field] = transformFieldSteps
                }
            }
        }

        return NadelExecutionPlan(
            operationExecutionContext = operationExecutionContext,
            transformFieldSteps = executionSteps,
            transformOperationContexts = transformOperationContexts,
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
