package graphql.nadel.tests

import graphql.nadel.Nadel
import graphql.nadel.tests.util.join
import graphql.nadel.tests.util.packageName
import graphql.nadel.tests.util.toSlug
import org.reflections.Reflections

interface EngineTestHook {
    fun makeNadel(engine: Engine, builder: Nadel.Builder): Nadel.Builder {
        return builder
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
    } catch (_: ClassNotFoundException) {
        println("No hook class found")
        return null
    }
    return hookClass.newInstance() as EngineTestHook
}

private object Util {
    val validated: Boolean by lazy(this::validate)

    private fun validate(): Boolean {
        val reflections = Reflections(hooksPackage)
        val hookImpls = reflections.getSubTypesOf(EngineTestHook::class.java)
        hookImpls.forEach { hookImpl ->
            javaClass.classLoader.getResource("fixtures/${hookImpl.simpleName}.yml")
                ?: error("Unable to find matching test for hook: ${hookImpl.simpleName}")
        }
        return true
    }
}

/**
 * The only reason this exists is so that you can use the "Suppress unused warning
 * if annotated by EngineTestHook" feature. Gets IntelliJ to stop complaining that
 * your hook class is unused.
 */
annotation class KeepHook
