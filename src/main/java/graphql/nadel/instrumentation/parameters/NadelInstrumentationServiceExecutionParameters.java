package graphql.nadel.instrumentation.parameters;


import graphql.execution.ExecutionContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.nadel.Service;

/**
 * Parameters sent to {@link graphql.nadel.instrumentation.NadelInstrumentation} methods
 */
@SuppressWarnings("TypeParameterUnusedInFormals")
public class NadelInstrumentationServiceExecutionParameters {
    private final Service service;
    private final ExecutionContext executionContext;
    private final InstrumentationState instrumentationState;

    public NadelInstrumentationServiceExecutionParameters(Service service, ExecutionContext executionContext, InstrumentationState instrumentationState) {
        this.service = service;
        this.executionContext = executionContext;
        this.instrumentationState = instrumentationState;
    }

    /**
     * Returns a cloned parameters object with the new state
     *
     * @param instrumentationState the new state for this parameters object
     *
     * @return a new parameters object with the new state
     */
    public NadelInstrumentationServiceExecutionParameters withNewState(InstrumentationState instrumentationState) {
        return new NadelInstrumentationServiceExecutionParameters(service, executionContext, instrumentationState);
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public <T extends InstrumentationState> T getInstrumentationState() {
        //noinspection unchecked
        return (T) instrumentationState;
    }
}
