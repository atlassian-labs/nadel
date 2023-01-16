package graphql.nadel.hints

fun interface InternalNamespaceTypenameResolutionHint {
    /**
     * Determines whether to internal [graphql.GraphQL] to resolve the `__typename` field
     * for namespaced fields.
     *
     * @param namespaceTypeName type name of the namespaced type e.g. IssueQuery
     * @return true to resolve typename internally
     */
    operator fun invoke(namespaceTypeName: String): Boolean
}
