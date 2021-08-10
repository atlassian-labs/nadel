package graphql.nadel.instrumentation.parameters;

import graphql.PublicApi;
import graphql.execution.ExecutionContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.normalized.ExecutableNormalizedOperation;
import org.jetbrains.annotations.Nullable;

/**
 * Parameters sent to {@link graphql.nadel.instrumentation.NadelInstrumentation} methods
 */
@PublicApi
public class NadelInstrumentRootExecutionResultParameters {

    private final ExecutionContext executionContext;
    private final InstrumentationState instrumentationState;
    @Nullable
    private final ExecutableNormalizedOperation query;

    public NadelInstrumentRootExecutionResultParameters(
            ExecutionContext executionContext, InstrumentationState instrumentationState, ExecutableNormalizedOperation query
    ) {
        this.executionContext = executionContext;
        this.instrumentationState = instrumentationState;
        this.query = query;
    }

    public NadelInstrumentRootExecutionResultParameters(ExecutionContext executionContext, InstrumentationState instrumentationState) {
        this(executionContext, instrumentationState, null);
    }

    /**
     * Returns a cloned parameters object with the new state
     *
     * @param instrumentationState the new state for this parameters object
     * @return a new parameters object with the new state
     */
    public NadelInstrumentRootExecutionResultParameters withNewState(InstrumentationState instrumentationState) {
        return new NadelInstrumentRootExecutionResultParameters(executionContext, instrumentationState, query);
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    @Nullable
    public ExecutableNormalizedOperation getQuery() {
        return query;
    }

    public <T extends InstrumentationState> T getInstrumentationState() {
        // noinspection unchecked
        return (T) instrumentationState;
    }

}
