package graphql.nadel.hints

import graphql.nadel.Service

fun interface NadelSharedTypeRenamesHint {
    /**
     * handle renaming of shared types in unions
     */
    operator fun invoke(service: Service): Boolean
}
