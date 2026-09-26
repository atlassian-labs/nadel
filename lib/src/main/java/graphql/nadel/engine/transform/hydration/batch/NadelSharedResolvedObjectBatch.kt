package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.transform.query.NadelQueryPath

/**
 * One aliased backing query result from a packed shared hydration operation.
 *
 * This is deliberately separate from [NadelResolvedObjectBatch], so isolated hydration keeps
 * its existing model and indexing path when coalescing is disabled.
 */
internal data class NadelSharedResolvedObjectBatch(
    val result: ServiceExecutionResult,
    val resultPath: NadelQueryPath,
)
