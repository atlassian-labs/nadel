package graphql.nadel.tests

enum class NadelEngineType {
    current,
    nextgen,
}

interface NadelEngineTypeValueProvider<T> {
    val current: T
    val nextgen: T

    operator fun get(engine: NadelEngineType): T {
        return when (engine) {
            NadelEngineType.current -> current
            NadelEngineType.nextgen -> nextgen
        }
    }
}

