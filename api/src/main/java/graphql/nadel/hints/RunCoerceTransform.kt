package graphql.nadel.hints

import graphql.nadel.Service

fun interface RunCoerceTransform {
    /**
     * Determines whether to run graphql.nadel.enginekt.transform.NadelCoerceTransform
     *
     * @param service the service in question
     * @return true to run the coerce transform
     */
    operator fun invoke(service: Service): Boolean
}
