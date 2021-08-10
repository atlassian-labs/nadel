package graphql.nadel.instrumentation;

import graphql.nadel.instrumentation.parameters.NadelInstrumentRootExecutionResultParameters;

public interface NadelEngineInstrumentation extends NadelInstrumentation {
    default void instrumentRootExecutionResult(Object rootExecutionResult, NadelInstrumentRootExecutionResultParameters parameters) {
    }
}
