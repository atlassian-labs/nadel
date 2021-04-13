package graphql.nadel.instrumentation;

import graphql.nadel.instrumentation.parameters.NadelInstrumentRootExecutionResultParameters;
import graphql.nadel.result.RootExecutionResultNode;

public interface EngineNadelInstrumentation extends NadelInstrumentation {
    default RootExecutionResultNode instrumentRootExecutionResult(RootExecutionResultNode rootExecutionResultNode, NadelInstrumentRootExecutionResultParameters parameters) {
        return rootExecutionResultNode;
    }
}
