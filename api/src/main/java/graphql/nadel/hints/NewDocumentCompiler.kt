package graphql.nadel.hints

import graphql.nadel.Service

fun interface NewDocumentCompiler {
    /**
     * Determines whether to use the compatability [graphql.nadel.compat.ExecutableNormalizedOperationToAstCompiler].
     *
     * @param service the service in question
     * @return true to use new document printer, false to use previous version
     */
    operator fun invoke(service: Service): Boolean
}
