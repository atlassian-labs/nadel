package graphql.nadel.engine.transform.result.json

import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.result.NadelResultPath
import graphql.nadel.result.NadelResultPathSegment

/**
 * Describes how a JSON payload is anchored in the operation and response.
 *
 * [queryPrefix] locates the payload in the normalized operation. [responsePrefix] locates the
 * payload in the concrete response and therefore retains list indices. A null [responsePrefix]
 * explicitly means that this payload cannot be mapped to one concrete response location.
 */
internal data class NadelResultScope(
    val queryPrefix: NadelQueryPath,
    val responsePrefix: NadelResultPath?,
) {
    companion object {
        val root = NadelResultScope(
            queryPrefix = NadelQueryPath.root,
            responsePrefix = NadelResultPath.empty,
        )

        /**
         * Makes a scope for an incremental payload.
         *
         * Query paths contain object segments only, while response paths retain every object and
         * array segment from the incremental payload anchor.
         */
        fun deferred(path: List<Any>): NadelResultScope {
            return NadelResultScope(
                queryPrefix = NadelQueryPath.fromResultPath(path),
                responsePrefix = NadelResultPath(
                    path.map { segment ->
                        when (segment) {
                            is String -> NadelResultPathSegment.Object(segment)
                            is Number -> NadelResultPathSegment.Array(segment.toInt())
                            else -> error(
                                "Unsupported defer result path segment '${segment.javaClass.name}'",
                            )
                        }
                    },
                ),
            )
        }

        fun unaddressable(
            queryPrefix: NadelQueryPath = NadelQueryPath.root,
        ): NadelResultScope {
            return NadelResultScope(
                queryPrefix = queryPrefix,
                responsePrefix = null,
            )
        }
    }
}
