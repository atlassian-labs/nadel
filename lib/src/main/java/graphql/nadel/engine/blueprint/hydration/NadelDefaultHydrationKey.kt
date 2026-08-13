package graphql.nadel.engine.blueprint.hydration

/**
 * Identifies the type-level `@defaultHydration` declaration from which an `@idHydrated`
 * instruction was synthesized.
 *
 * A type can declare at most one default hydration, so its overall-schema type name is a stable
 * declaration identity. Runtime compatibility is deliberately evaluated separately.
 */
internal data class NadelDefaultHydrationKey(
    val declaringTypeName: String,
)
