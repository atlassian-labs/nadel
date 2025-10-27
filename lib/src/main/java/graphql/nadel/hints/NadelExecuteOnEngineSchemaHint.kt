package graphql.nadel.hints

fun interface NadelExecuteOnEngineSchemaHint {
    /**
     * Determines whether a query is executed on the engine schema (true) or query schema (false).
     */
    operator fun invoke(): Boolean
}
