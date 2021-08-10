package graphql.nadel.instrumentation;

import graphql.execution.instrumentation.InstrumentationState;
import graphql.nadel.instrumentation.parameters.NadelInstrumentRootExecutionResultParameters;

import java.util.List;

public class ChainedNadelEngineInstrumentation extends ChainedNadelInstrumentation implements NadelEngineInstrumentation {
    public ChainedNadelEngineInstrumentation(List<NadelInstrumentation> instrumentations) {
        super(instrumentations);
    }

    @Override
    public void instrumentRootExecutionResult(Object rootExecutionResult, NadelInstrumentRootExecutionResultParameters parameters) {
        for (NadelInstrumentation instrumentation : getInstrumentations()) {
            if (instrumentation instanceof NadelEngineInstrumentation) {
                NadelEngineInstrumentation nadelEngineInstrumentation = (NadelEngineInstrumentation) instrumentation;
                InstrumentationState state = getStateFor(instrumentation, parameters.getInstrumentationState());
                nadelEngineInstrumentation.instrumentRootExecutionResult(rootExecutionResult, parameters.withNewState(state));
            }
        }
    }
}
