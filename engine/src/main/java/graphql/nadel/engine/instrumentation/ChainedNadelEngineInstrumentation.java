package graphql.nadel.engine.instrumentation;

import graphql.execution.instrumentation.InstrumentationState;
import graphql.nadel.engine.result.RootExecutionResultNode;
import graphql.nadel.instrumentation.ChainedNadelInstrumentation;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.instrumentation.parameters.NadelInstrumentRootExecutionResultParameters;

import java.util.List;

public class ChainedNadelEngineInstrumentation extends ChainedNadelInstrumentation implements NadelEngineInstrumentation {
    public ChainedNadelEngineInstrumentation(List<NadelInstrumentation> instrumentations) {
        super(instrumentations);
    }

    @Override
    public RootExecutionResultNode instrumentRootExecutionResult(RootExecutionResultNode rootExecutionResultNode, NadelInstrumentRootExecutionResultParameters parameters) {
        for (NadelInstrumentation instrumentation : getInstrumentations()) {
            if (instrumentation instanceof NadelEngineInstrumentation) {
                NadelEngineInstrumentation nadelEngineInstrumentation = (NadelEngineInstrumentation) instrumentation;
                InstrumentationState state = getStateFor(instrumentation, parameters.getInstrumentationState());
                rootExecutionResultNode = nadelEngineInstrumentation.instrumentRootExecutionResult(rootExecutionResultNode, parameters.withNewState(state));
            }
        }
        return rootExecutionResultNode;
    }
}
