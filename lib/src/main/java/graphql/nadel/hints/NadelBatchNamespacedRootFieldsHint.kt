package graphql.nadel.hints

import graphql.nadel.Service

fun interface NadelBatchNamespacedRootFieldsHint {
    /**
     * Enables batching across namespace wrappers in queries. Independent of [NadelBatchRootFieldsHint];
     * combining namespace wrappers with non-namespaced roots requires both hints to allow the service.
     */
    operator fun invoke(): Boolean

    /**
     * Per-service opt-in. The global check must also pass before namespace wrappers are batched.
     */
    operator fun invoke(service: Service): Boolean = invoke()
}
