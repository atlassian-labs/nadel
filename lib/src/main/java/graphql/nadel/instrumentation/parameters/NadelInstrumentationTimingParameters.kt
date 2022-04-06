package graphql.nadel.instrumentation.parameters

import graphql.execution.instrumentation.InstrumentationState
import graphql.nadel.engine.transform.NadelTransform
import java.time.Duration
import kotlin.reflect.KClass

data class NadelInstrumentationTimingParameters(
    val step: Step,
    val duration: Duration,
    /**
     * If an exception occurred during the timing of the step, then it is passed in here.
     */
    val exception: Throwable?,
    private val context: Any?,
    private val instrumentationState: InstrumentationState?,
) {
    fun <T> getContext(): T? {
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

    enum class RootStep(override val parent: Step? = null) : Step {
        ExecutionPlanning,
        QueryTransforming,
        ResultTransforming,
    }

    data class ChildStep internal constructor(
        override val parent: Step,
        override val name: String,
    ) : Step {
        // Util to create from a NadelTransform::class
        constructor(
            parent: Step,
            transform: KClass<out NadelTransform<*>>,
        ) : this(
            name = transform.simpleName ?: "AnonymousTransform", // If it's null it's an anonymous class
            parent = parent,
        )

        // Util to create from a NadelTransform::class
        constructor(
            parent: Step,
            transform: Class<out NadelTransform<*>>,
        ) : this(
            name = transform.simpleName ?: "AnonymousTransform", // If it's null it's an anonymous class
            parent = parent,
        )
    }
}
