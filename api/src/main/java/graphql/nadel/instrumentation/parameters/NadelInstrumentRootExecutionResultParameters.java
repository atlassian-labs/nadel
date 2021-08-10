package graphql.nadel.instrumentation.parameters;

import graphql.PublicApi;
import graphql.execution.ExecutionContext;
import graphql.execution.instrumentation.InstrumentationState;

/**
 * Parameters sent to {@link graphql.nadel.instrumentation.NadelInstrumentation} methods
 */
@PublicApi
public class NadelInstrumentRootExecutionResultParameters {

    private final ExecutionContext executionContext;
    private final InstrumentationState instrumentationState;

    public NadelInstrumentRootExecutionResultParameters(ExecutionContext executionContext, InstrumentationState instrumentationState) {
        this.executionContext = executionContext;
        this.instrumentationState = instrumentationState;
    }

    /**
     * Returns a cloned parameters object with the new state
     *
     * @param instrumentationState the new state for this parameters object
     * @return a new parameters object with the new state
     */
    public NadelInstrumentRootExecutionResultParameters withNewState(InstrumentationState instrumentationState) {
        return new NadelInstrumentRootExecutionResultParameters(executionContext, instrumentationState);
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public <T extends InstrumentationState> T getInstrumentationState() {
        // noinspection unchecked
        return (T) instrumentationState;
    }

}
