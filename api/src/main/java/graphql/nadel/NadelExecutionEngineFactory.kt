package graphql.nadel

fun interface NadelExecutionEngineFactory {
    fun create(nadel: Nadel): NadelExecutionEngine
}
