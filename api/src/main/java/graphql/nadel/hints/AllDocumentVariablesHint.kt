package graphql.nadel.hints

import graphql.nadel.Service

fun interface AllDocumentVariablesHint {
    /**
     * Determines whether to use all variables for arguments in the document sent to a service
     *
     * @param service the service in question
     * @return true to use all variables
     */
    operator fun invoke(service: Service): Boolean
}
