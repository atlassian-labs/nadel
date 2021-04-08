package graphql.nadel.instrumentation.parameters;

import graphql.ExecutionInput;
import graphql.PublicApi;
import graphql.schema.GraphQLSchema;

/**
 * Parameters sent to {@link graphql.nadel.instrumentation.NadelInstrumentation} methods
 */
@PublicApi
public class NadelInstrumentationCreateStateParameters {
    private final GraphQLSchema schema;
    private final ExecutionInput executionInput;

    public NadelInstrumentationCreateStateParameters(GraphQLSchema schema, ExecutionInput executionInput) {
        this.schema = schema;
        this.executionInput = executionInput;
    }

    public GraphQLSchema getSchema() {
        return schema;
    }

    public ExecutionInput getExecutionInput() {
        return executionInput;
    }
}
