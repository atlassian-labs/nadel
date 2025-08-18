package graphql.nadel

import graphql.GraphQLContext
import graphql.execution.ExecutionId
import graphql.nadel.time.NadelInternalLatencyTracker
import graphql.nadel.time.NadelInternalLatencyTrackerImpl
import graphql.nadel.time.NadelStopwatch
import java.util.function.Consumer

data class NadelExecutionInput(
    val query: String,
    val operationName: String?,
    val context: Any?,
    val graphqlContext: GraphQLContext,
    val variables: Map<String, Any?>,
    val extensions: Map<String, Any?>,
    val executionId: ExecutionId?,
    val executionHints: NadelExecutionHints,
    val latencyTracker: NadelInternalLatencyTracker,
) {
    class Builder {
        private var query: String? = null
        private var operationName: String? = null
        private var context: Any? = null
        private var graphqlContextBuilder: GraphQLContext.Builder = GraphQLContext.newContext()
        private var variables: Map<String, Any?> = LinkedHashMap()
        private var extensions: Map<String, Any?> = LinkedHashMap()
        private var executionId: ExecutionId? = null
        private var executionHints = NadelExecutionHints.newHints().build()
        private var latencyTracker: NadelInternalLatencyTracker? = null

        fun query(query: String): Builder {
            this.query = query
            return this
        }

        fun operationName(operationName: String?): Builder {
            this.operationName = operationName
            return this
        }

        fun context(context: Any?): Builder {
            this.context = context
            return this
        }

        fun graphqlContext(contextConsumer: Consumer<GraphQLContext.Builder>): Builder {
            contextConsumer.accept(this.graphqlContextBuilder)
            return this
        }

        fun graphqlContext(key: Any, value: Any): Builder {
            this.graphqlContextBuilder.put(key, value)
            return this
        }

        fun variables(variables: Map<String, Any?>?): Builder {
            this.variables = variables ?: emptyMap()
            return this
        }

        fun extensions(extensions: Map<String, Any?>?): Builder {
            this.extensions = extensions ?: emptyMap()
            return this
        }

        fun executionId(executionId: ExecutionId): Builder {
            this.executionId = executionId
            return this
        }

        fun executionHints(nadelExecutionHints: NadelExecutionHints): Builder {
            this.executionHints = nadelExecutionHints
            return this
        }

        fun transformExecutionHints(transformFunc: (NadelExecutionHints.Builder) -> Unit): Builder {
            this.executionHints = executionHints.toBuilder()
                .also(transformFunc)
                .build()
            return this
        }

        fun latencyTracker(latencyTracker: NadelInternalLatencyTracker): Builder {
            this.latencyTracker = latencyTracker
            return this
        }

        fun build(): NadelExecutionInput {
            return NadelExecutionInput(
                query = requireNotNull(query) { "Query must be provided" },
                operationName = operationName,
                context = context,
                graphqlContext = graphqlContextBuilder.build(),
                variables = variables,
                extensions = extensions,
                executionId = executionId,
                executionHints = executionHints,
                latencyTracker = latencyTracker ?: NadelInternalLatencyTrackerImpl(NadelStopwatch()),
            )
        }
    }

    companion object {
        @JvmStatic
        fun newNadelExecutionInput(): Builder {
            return Builder()
        }
    }
}
