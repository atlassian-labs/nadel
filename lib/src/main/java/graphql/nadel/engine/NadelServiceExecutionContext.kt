package graphql.nadel.engine

/**
 * Base class for context objects created for one service execution.
 *
 * This is created for each portion of the query we execute.
 *
 * This is NOT shared with other executions to the same service.
 */
abstract class NadelServiceExecutionContext {
    internal object None : NadelServiceExecutionContext()
}
