package graphql.nadel.result;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.result.ResolvedValue;

import java.util.Collections;

public class UnresolvedObjectResultNode extends ObjectExecutionResultNode {

    public UnresolvedObjectResultNode(ExecutionStepInfo executionStepInfo, ResolvedValue resolvedValue) {
        super(executionStepInfo, resolvedValue, Collections.emptyList(), Collections.emptyList());
    }

}