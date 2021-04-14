package graphql.nadel.engine.instrumentation;

import graphql.nadel.engine.result.RootExecutionResultNode;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.instrumentation.parameters.NadelInstrumentRootExecutionResultParameters;

public interface EngineNadelInstrumentation extends NadelInstrumentation {
    default RootExecutionResultNode instrumentRootExecutionResult(RootExecutionResultNode rootExecutionResultNode, NadelInstrumentRootExecutionResultParameters parameters) {
        return rootExecutionResultNode;
    }
}
