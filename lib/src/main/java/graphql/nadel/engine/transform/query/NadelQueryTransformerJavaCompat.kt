package graphql.nadel.engine.transform.query

import graphql.nadel.engine.compiler.NadelExecutableNormalizedField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class NadelQueryTransformerJavaCompat(
    internal val queryTransformer: NadelQueryTransformer,
    private val coroutineScope: CoroutineScope,
) {
    fun transform(
        fields: List<NadelExecutableNormalizedField>,
    ): CompletableFuture<List<NadelExecutableNormalizedField>> {
        return coroutineScope.future {
            queryTransformer.transform(fields)
        }
    }

    fun transform(
        field: NadelExecutableNormalizedField,
    ): CompletableFuture<List<NadelExecutableNormalizedField>> {
        return coroutineScope.future {
            queryTransformer.transform(field)
        }
    }
}
