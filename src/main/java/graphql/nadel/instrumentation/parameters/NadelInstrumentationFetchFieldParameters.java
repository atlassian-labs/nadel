package graphql.nadel.instrumentation.parameters;


import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.instrumentation.InstrumentationState;

/**
 * Parameters sent to {@link graphql.nadel.instrumentation.NadelInstrumentation} methods
 */
@SuppressWarnings("TypeParameterUnusedInFormals")
public class NadelInstrumentationFetchFieldParameters {
    private final ExecutionContext executionContext;
    private final ExecutionStepInfo executionStepInfo;
    private final InstrumentationState instrumentationState;

    public NadelInstrumentationFetchFieldParameters(ExecutionContext executionContext, ExecutionStepInfo executionStepInfo, InstrumentationState instrumentationState) {
        this.executionContext = executionContext;
        this.executionStepInfo = executionStepInfo;
        this.instrumentationState = instrumentationState;
    }

    /**
     * Returns a cloned parameters object with the new state
     *
     * @param instrumentationState the new state for this parameters object
     *
     * @return a new parameters object with the new state
     */
    public NadelInstrumentationFetchFieldParameters withNewState(InstrumentationState instrumentationState) {
        return new NadelInstrumentationFetchFieldParameters(executionContext, executionStepInfo, instrumentationState);
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public ExecutionStepInfo getExecutionStepInfo() {
        return executionStepInfo;
    }

    public <T extends InstrumentationState> T getInstrumentationState() {
        //noinspection unchecked
        return (T) instrumentationState;
    }
}
