package graphql.nadel.hints

fun interface NadelExecutableServiceMigrationHint {
    operator fun invoke(): Boolean
}
