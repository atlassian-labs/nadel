package graphql.nadel.engine.transform.partition

/**
 * Base class for context objects created for partition resolution.
 *
 * An instance is created for each field that is subject to partitioning.
 */
abstract class NadelFieldPartitionContext {
    internal object None : NadelFieldPartitionContext()
}
