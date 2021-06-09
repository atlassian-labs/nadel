package graphql.nadel.tests.util.fixtures

import graphql.nadel.Nadel
import graphql.nadel.tests.Engine

interface EngineTestHook {
    fun makeNadel(engine: Engine, builder: Nadel.Builder): Nadel.Builder {
        return builder
    }
}

/**
 * The only reason this exists is so that you can use the "Suppress unused warning
 * if annotated by EngineTestHook" feature. Gets IntelliJ to stop complaining that
 * your hook class is unused.
 */
annotation class KeepHook
