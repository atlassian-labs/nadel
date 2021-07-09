package graphql.nadel.tests

enum class NadelEngineType {
    current,
    nextgen,
}

interface NadelEngineTypeValueProvider<T> {
    val current: T
    val nextgen: T

    operator fun get(engineType: NadelEngineType): T {
        return when (engineType) {
            NadelEngineType.current -> current
            NadelEngineType.nextgen -> nextgen
        }
    }
}

