package graphql.nadel.result;

import graphql.Internal;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.NonNullableFieldWasNullException;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.ResolvedValue;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static graphql.nadel.result.LeafExecutionResultNode.newLeafExecutionResultNode;
import static graphql.nadel.result.UnresolvedObjectResultNode.newUnresolvedExecutionResultNode;
import static java.util.stream.Collectors.toList;

@Internal
public class ResultNodesCreator {

    public ExecutionResultNode createResultNode(FetchedValueAnalysis fetchedValueAnalysis) {
        ResolvedValue resolvedValue = createResolvedValue(fetchedValueAnalysis);
        ExecutionStepInfo executionStepInfo = fetchedValueAnalysis.getExecutionStepInfo();

        if (fetchedValueAnalysis.isNullValue() && executionStepInfo.isNonNullType()) {
            NonNullableFieldWasNullException nonNullableFieldWasNullException = new NonNullableFieldWasNullException(executionStepInfo, executionStepInfo.getPath());

            return newLeafExecutionResultNode()
                    .executionStepInfo(executionStepInfo)
                    .resolvedValue(resolvedValue)
                    .nonNullableFieldWasNullException(nonNullableFieldWasNullException)
                    .build();
        }
        if (fetchedValueAnalysis.isNullValue()) {
            return newLeafExecutionResultNode()
                    .executionStepInfo(executionStepInfo)
                    .resolvedValue(resolvedValue)
                    .nonNullableFieldWasNullException(null)
                    .build();
        }
        if (fetchedValueAnalysis.getValueType() == FetchedValueAnalysis.FetchedValueType.OBJECT) {
            return createUnresolvedNode(fetchedValueAnalysis);
        }
        if (fetchedValueAnalysis.getValueType() == FetchedValueAnalysis.FetchedValueType.LIST) {
            return createListResultNode(fetchedValueAnalysis);
        }
        return newLeafExecutionResultNode()
                .executionStepInfo(executionStepInfo)
                .resolvedValue(resolvedValue)
                .nonNullableFieldWasNullException(null)
                .build();
    }

    private ExecutionResultNode createUnresolvedNode(FetchedValueAnalysis fetchedValueAnalysis) {
        return newUnresolvedExecutionResultNode()
                .executionStepInfo(fetchedValueAnalysis.getExecutionStepInfo())
                .resolvedValue(createResolvedValue(fetchedValueAnalysis))
                .build();
    }

    private ResolvedValue createResolvedValue(FetchedValueAnalysis fetchedValueAnalysis) {
        return ResolvedValue.newResolvedValue()
                .completedValue(fetchedValueAnalysis.getCompletedValue())
                .localContext(fetchedValueAnalysis.getFetchedValue().getLocalContext())
                .nullValue(fetchedValueAnalysis.isNullValue())
                .errors(fetchedValueAnalysis.getErrors())
                .build();
    }

    private Optional<NonNullableFieldWasNullException> getFirstNonNullableException(Collection<ExecutionResultNode> collection) {
        return collection.stream()
                .filter(executionResultNode -> executionResultNode.getNonNullableFieldWasNullException() != null)
                .map(ExecutionResultNode::getNonNullableFieldWasNullException)
                .findFirst();
    }

    private ExecutionResultNode createListResultNode(FetchedValueAnalysis fetchedValueAnalysis) {
        List<ExecutionResultNode> executionResultNodes = fetchedValueAnalysis
                .getChildren()
                .stream()
                .map(this::createResultNode)
                .collect(toList());
        return ListExecutionResultNode.newListExecutionResultNode()
                .executionStepInfo(fetchedValueAnalysis.getExecutionStepInfo())
                .resolvedValue(createResolvedValue(fetchedValueAnalysis))
                .children(executionResultNodes)
                .build();
    }
}
