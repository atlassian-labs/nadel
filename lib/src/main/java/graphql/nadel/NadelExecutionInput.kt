package graphql.nadel

import graphql.execution.ExecutionId

class NadelExecutionInput private constructor(
    val query: String,
    val operationName: String?,
    val context: Any?,
    val variables: Map<String, Any?>,
    val executionId: ExecutionId?,
    val nadelExecutionHints: NadelExecutionHints,
) {
    class Builder {
        private var query: String? = null
        private var operationName: String? = null
        private var context: Any? = null
        private var variables: Map<String, Any?> = LinkedHashMap()
        private var executionId: ExecutionId? = null
        private var executionHints = NadelExecutionHints.newHints().build()

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

        fun variables(variables: Map<String, Any?>?): Builder {
            this.variables = variables ?: emptyMap()
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

        fun build(): NadelExecutionInput {
            return NadelExecutionInput(
                query = requireNotNull(query) { "Query must be provided" },
                operationName = operationName,
                context = context,
                variables = variables,
                executionId = executionId,
                nadelExecutionHints = executionHints,
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
