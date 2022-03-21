package graphql.nadel.engine.transform.query

import graphql.normalized.ExecutableNormalizedField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class NadelQueryTransformerJavaCompat(
    private val queryTransformer: NadelQueryTransformer,
    private val coroutineScope: CoroutineScope,
) {
    fun transform(
        fields: List<ExecutableNormalizedField>,
    ): CompletableFuture<List<ExecutableNormalizedField>> {
        return coroutineScope.future {
            queryTransformer.transform(fields)
        }
    }

    fun transform(
        field: ExecutableNormalizedField,
    ): CompletableFuture<List<ExecutableNormalizedField>> {
        return coroutineScope.future {
            queryTransformer.transform(field)
        }
    }
}
