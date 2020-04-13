package graphql.nadel.result;

import graphql.Internal;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.NonNullableFieldWasNullException;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.ResolvedValue;
import graphql.nadel.dsl.NodeId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static graphql.nadel.dsl.NodeId.getIds;
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
                    .alias(executionStepInfo.getField().getSingleField().getAlias())
                    .fieldIds(getIds(executionStepInfo.getField()))
                    .objectType(executionStepInfo.getFieldContainer())
                    .fieldDefinition(executionStepInfo.getFieldDefinition())
                    .executionPath(executionStepInfo.getPath())
                    .resolvedValue(resolvedValue)
                    .nonNullableFieldWasNullException(nonNullableFieldWasNullException)
                    .build();
        }
        if (fetchedValueAnalysis.isNullValue()) {
            return newLeafExecutionResultNode()
                    .alias(executionStepInfo.getField().getSingleField().getAlias())
                    .fieldIds(getIds(executionStepInfo.getField()))
                    .objectType(executionStepInfo.getFieldContainer())
                    .fieldDefinition(executionStepInfo.getFieldDefinition())
                    .executionPath(executionStepInfo.getPath())
                    .resolvedValue(resolvedValue)
                    .build();
        }
        if (fetchedValueAnalysis.getValueType() == FetchedValueAnalysis.FetchedValueType.OBJECT) {
            return createUnresolvedNode(fetchedValueAnalysis);
        }
        if (fetchedValueAnalysis.getValueType() == FetchedValueAnalysis.FetchedValueType.LIST) {
            return createListResultNode(fetchedValueAnalysis);
        }
        return newLeafExecutionResultNode()
                .alias(executionStepInfo.getField().getSingleField().getAlias())
                .fieldIds(NodeId.getIds(executionStepInfo.getField()))
                .objectType(executionStepInfo.getFieldContainer())
                .fieldDefinition(executionStepInfo.getFieldDefinition())
                .executionPath(executionStepInfo.getPath())
                .resolvedValue(resolvedValue)
                .build();
    }

    private ExecutionResultNode createUnresolvedNode(FetchedValueAnalysis fetchedValueAnalysis) {
        return newUnresolvedExecutionResultNode()
                .executionStepInfo(fetchedValueAnalysis.getExecutionStepInfo())
                .executionPath(fetchedValueAnalysis.getExecutionStepInfo().getPath())
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
        ExecutionStepInfo executionStepInfo = fetchedValueAnalysis.getExecutionStepInfo();
        return ListExecutionResultNode.newListExecutionResultNode()
                .alias(executionStepInfo.getField().getSingleField().getAlias())
                .fieldIds(NodeId.getIds(executionStepInfo.getField()))
                .objectType(executionStepInfo.getFieldContainer())
                .fieldDefinition(executionStepInfo.getFieldDefinition())
                .executionPath(executionStepInfo.getPath())
                .resolvedValue(createResolvedValue(fetchedValueAnalysis))
                .children(executionResultNodes)
                .build();
    }
}
