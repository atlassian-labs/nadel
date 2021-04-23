package graphql.nadel.instrumentation.parameters;


import graphql.PublicApi;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.nadel.normalized.NormalizedQueryFromAst;

/**
 * Parameters sent to {@link graphql.nadel.instrumentation.NadelInstrumentation} methods
 */
@SuppressWarnings("TypeParameterUnusedInFormals")
@PublicApi
public class NadelInstrumentationExecuteOperationParameters {
    private final InstrumentationState instrumentationState;
    private final NormalizedQueryFromAst normalizedQueryFromAst;

    public NadelInstrumentationExecuteOperationParameters(
            NormalizedQueryFromAst normalizedQueryFromAst,
            InstrumentationState instrumentationState
    ) {
        this.instrumentationState = instrumentationState;
        this.normalizedQueryFromAst = normalizedQueryFromAst;
    }

    /**
     * Returns a cloned parameters object with the new state
     *
     * @param instrumentationState the new state for this parameters object
     * @return a new parameters object with the new state
     */
    public NadelInstrumentationExecuteOperationParameters withNewState(InstrumentationState instrumentationState) {
        return new NadelInstrumentationExecuteOperationParameters(normalizedQueryFromAst, instrumentationState);
    }

    public NormalizedQueryFromAst getNormalizedQueryFromAst() {
        return normalizedQueryFromAst;
    }

    public <T extends InstrumentationState> T getInstrumentationState() {
        //noinspection unchecked
        return (T) instrumentationState;
    }
}
