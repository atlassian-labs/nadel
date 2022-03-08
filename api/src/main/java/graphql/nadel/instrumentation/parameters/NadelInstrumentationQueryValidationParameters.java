package graphql.nadel.instrumentation.parameters;

import graphql.ExecutionInput;
import graphql.PublicApi;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.language.Document;
import graphql.schema.GraphQLSchema;

/**
 * Parameters sent to {@link graphql.nadel.instrumentation.NadelInstrumentation} methods
 */
@PublicApi
public class NadelInstrumentationQueryValidationParameters extends NadelInstrumentationQueryExecutionParameters {
    private final Document document;

    public NadelInstrumentationQueryValidationParameters(ExecutionInput executionInput, Document document, GraphQLSchema schema, InstrumentationState instrumentationState) {
        super(executionInput, schema, instrumentationState);
        this.document = document;
    }

    /**
     * Returns a cloned parameters object with the new state
     *
     * @param instrumentationState the new state for this parameters object
     *
     * @return a new parameters object with the new state
     */
    @Override
    public NadelInstrumentationQueryValidationParameters withNewState(InstrumentationState instrumentationState) {
        return new NadelInstrumentationQueryValidationParameters(
                this.getExecutionInput(), document, getSchema(), instrumentationState);
    }

    public Document getDocument() {
        return document;
    }
}
