package graphql.nadel.tests

import graphql.ExecutionResult
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionHints
import graphql.nadel.NadelExecutionInput
import graphql.nadel.ServiceExecution
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.schema.NeverWiringFactory
import graphql.nadel.schema.SchemaTransformationHook
import graphql.nadel.tests.util.join
import graphql.nadel.tests.util.toSlug
import graphql.nadel.validation.NadelSchemaValidationError
import graphql.schema.idl.WiringFactory
import org.reflections.Reflections
import java.io.File

interface EngineTestHook {
    companion object {
        /**
         * Used when there is no test hook for a class.
         *
         * We use this to ensure the default behaviour is the same.
         *
         * i.e. other classes don't start redefining the default behaviour when the hook is null.
         */
        val noOp = object : EngineTestHook {}
    }

    val customTransforms: List<NadelTransform<out Any>>
        get() = emptyList()

    val schemaTransformationHook: SchemaTransformationHook
        get() = SchemaTransformationHook.Identity

    val wiringFactory: WiringFactory
        get() = NeverWiringFactory()

    fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
    }

    fun makeExecutionInput(
        builder: NadelExecutionInput.Builder,
    ): NadelExecutionInput.Builder {
        return builder
    }

    fun makeExecutionHints(
        builder: NadelExecutionHints.Builder,
    ): NadelExecutionHints.Builder {
        return builder
    }

    fun isSchemaValid(errors: Set<NadelSchemaValidationError>): Boolean {
        return errors.isEmpty()
    }

    fun assertResult(result: ExecutionResult) {
    }

    fun assertFailure(throwable: Throwable): Boolean {
        return false
    }

    /**
     * Allows you to wrap the base test service execution call, so you can do things before or after it
     */
    fun wrapServiceExecution(baseTestServiceExecution: ServiceExecution) : ServiceExecution {
        return baseTestServiceExecution
    }
}

private val hooksPackage: String = join(
    EngineTestHook::class.java.packageName,
    "hooks",
    separator = ".",
)

internal fun getTestHook(fixture: TestFixture): EngineTestHook? {
    require(Util.validated) { "Tests hooks are not valid" }

    val hookClass = try {
        Class.forName(
            join(hooksPackage, fixture.name.toSlug(), separator = "."),
        )
    } catch (e: ClassNotFoundException) {
        println("No hook class found: ${e.message}")
        return null
    }

    return hookClass.newInstance() as EngineTestHook
}

private object Util {
    val validated: Boolean by lazy(this::validate)

    private fun validate(): Boolean {
        val reflections = Reflections(hooksPackage)
        val hookImpls = reflections.getSubTypesOf(EngineTestHook::class.java)

        // TODO: provide single source of truth for this logic - duplicated in EngineTests
        @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val allFixtureFileNames = File(javaClass.classLoader.getResource("fixtures").path)
            .walkTopDown()
            .filter { it.extension == "yml" }
            .map { it.nameWithoutExtension }
            .toHashSet()

        hookImpls
            .filter { it.isAnnotationPresent(UseHook::class.java) }
            .forEach { hookImpl ->
                val fixtureName = hookImpl.simpleName
                if (fixtureName !in allFixtureFileNames) {
                    error("Unable to find matching test for hook: $fixtureName")
                }
            }

        return true
    }
}

/**
 * Indicates that the class is a test hook.
 *
 * Allows developers to create subclasses of EngineTestHook that do not map to a specific test. This is useful
 * when you have a set of tests that share common hook code, so you can place this common hook in a class that
 * extends EngineTestHook and don't use the KeepHook annotation in it.
 *
 * Dev Hint: You can use the "Suppress unused warning if annotated by EngineTestHook" feature. Gets IntelliJ
 * to stop complaining that your hook class is unused.
 */
@Target(AnnotationTarget.CLASS)
annotation class UseHook
