package graphql.nadel.instrumentation.parameters

import graphql.execution.instrumentation.InstrumentationState
import graphql.nadel.NadelUserContext
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.ChildStep
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.Step
import java.time.Duration

data class NadelInstrumentationTimingParameters(
    val step: Step,
    /**
     * Can be null for batched timings which don't really have a start time.
     */
    val internalLatency: Duration,
    /**
     * If an exception occurred during the timing of the step, then it is passed in here.
     */
    val exception: Throwable?,
    private val context: NadelUserContext?,
    private val instrumentationState: InstrumentationState?,
) {
    fun <T : NadelUserContext> getContext(): T? {
        @Suppress("UNCHECKED_CAST") // trust the caller
        return context as T
    }

    fun <T : InstrumentationState?> getInstrumentationState(): T? {
        @Suppress("UNCHECKED_CAST") // trust the caller
        return instrumentationState as T?
    }

    sealed interface Step {
        val parent: Step?
        val name: String

        fun getFullName(): String {
            val parents = mutableListOf<Step>()

            var cursor: Step? = this
            while (cursor != null) {
                parents.add(cursor)
                cursor = cursor.parent
            }

            return parents
                .asReversed()
                .joinToString(separator = ".") { it.name }
        }

        /**
         * Determines whether `this` is a child of another [parent] step.
         */
        fun isChildOf(parent: Step): Boolean {
            var cursor: Step? = this

            while (cursor != null && cursor != parent) {
                cursor = cursor.parent
            }

            return cursor == parent
        }
    }

    enum class RootStep : Step {
        ExecutableOperationParsing,
        ExecutionPlanning,
        QueryTransforming,
        ResultTransforming,
        ServiceExecution,
        ;

        override val parent: Step? = null
    }

    data class ChildStep internal constructor(
        override val parent: Step,
        override val name: String,
    ) : Step {
        constructor(
            parent: Step,
            transform: NadelTransform<*>,
        ) : this(
            parent = parent,
            name = transform.name,
        )

        companion object {
            val DocumentCompilation = RootStep.ServiceExecution.child("DocumentCompilation")
        }
    }
}

internal fun Step.child(name: String): ChildStep {
    return ChildStep(parent = this, name = name)
}
