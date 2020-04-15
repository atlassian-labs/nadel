package graphql.nadel.result;

import graphql.Internal;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.NonNullableFieldWasNullException;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.ResolvedValue;
import graphql.nadel.normalized.NormalizedQueryField;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static graphql.nadel.dsl.NodeId.getIds;
import static graphql.nadel.result.LeafExecutionResultNode.newLeafExecutionResultNode;
import static graphql.nadel.result.UnresolvedObjectResultNode.newUnresolvedExecutionResultNode;
import static java.util.stream.Collectors.toList;

@Internal
public class ResultNodesCreator {

    public ExecutionResultNode createResultNode(FetchedValueAnalysis fetchedValueAnalysis, NormalizedQueryField normalizedQueryField) {
        ResolvedValue resolvedValue = createResolvedValue(fetchedValueAnalysis);
        ExecutionStepInfo executionStepInfo = fetchedValueAnalysis.getExecutionStepInfo();

        if (fetchedValueAnalysis.isNullValue() && executionStepInfo.isNonNullType()) {
            NonNullableFieldWasNullException nonNullableFieldWasNullException = new NonNullableFieldWasNullException(executionStepInfo, executionStepInfo.getPath());
            LeafExecutionResultNode result = newLeafExecutionResultNode()
                    .executionPath(executionStepInfo.getPath())
                    .alias(executionStepInfo.getField().getSingleField().getAlias())
                    .fieldIds(getIds(executionStepInfo.getField()))
                    .objectType(executionStepInfo.getFieldContainer())
                    .fieldDefinition(executionStepInfo.getFieldDefinition())
                    .resolvedValue(resolvedValue)
                    .nonNullableFieldWasNullException(nonNullableFieldWasNullException)
                    .build();
            return result;
        }
        if (fetchedValueAnalysis.isNullValue()) {
            LeafExecutionResultNode result = newLeafExecutionResultNode()
                    .alias(executionStepInfo.getField().getSingleField().getAlias())
                    .fieldIds(getIds(executionStepInfo.getField()))
                    .objectType(executionStepInfo.getFieldContainer())
                    .fieldDefinition(executionStepInfo.getFieldDefinition())
                    .executionPath(executionStepInfo.getPath())
                    .resolvedValue(resolvedValue)
                    .build();
            return result;
        }
        if (fetchedValueAnalysis.getValueType() == FetchedValueAnalysis.FetchedValueType.OBJECT) {
            return createUnresolvedNode(fetchedValueAnalysis, normalizedQueryField);
        }
        if (fetchedValueAnalysis.getValueType() == FetchedValueAnalysis.FetchedValueType.LIST) {
            return createListResultNode(fetchedValueAnalysis, normalizedQueryField);
        }
        LeafExecutionResultNode result = newLeafExecutionResultNode()
                .alias(executionStepInfo.getField().getSingleField().getAlias())
                .fieldIds(getIds(executionStepInfo.getField()))
                .objectType(executionStepInfo.getFieldContainer())
                .fieldDefinition(executionStepInfo.getFieldDefinition())
                .executionPath(executionStepInfo.getPath())
                .resolvedValue(resolvedValue)
                .build();
        return result;
    }

    private ExecutionResultNode createUnresolvedNode(FetchedValueAnalysis fetchedValueAnalysis, NormalizedQueryField normalizedQueryField) {
        UnresolvedObjectResultNode result = newUnresolvedExecutionResultNode()
                .executionPath(fetchedValueAnalysis.getExecutionStepInfo().getPath())
                .executionStepInfo(fetchedValueAnalysis.getExecutionStepInfo())
                .normalizedField(normalizedQueryField)
                .resolvedValue(createResolvedValue(fetchedValueAnalysis))
                .build();
        return result;
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

    private ExecutionResultNode createListResultNode(FetchedValueAnalysis fetchedValueAnalysis, NormalizedQueryField normalizedQueryField) {
        List<ExecutionResultNode> executionResultNodes = fetchedValueAnalysis
                .getChildren()
                .stream()
                .map(child -> createResultNode(child, normalizedQueryField))
                .collect(toList());
        ExecutionStepInfo executionStepInfo = fetchedValueAnalysis.getExecutionStepInfo();
        ListExecutionResultNode result = ListExecutionResultNode.newListExecutionResultNode()
                .alias(executionStepInfo.getField().getSingleField().getAlias())
                .fieldIds(getIds(executionStepInfo.getField()))
                .objectType(executionStepInfo.getFieldContainer())
                .fieldDefinition(executionStepInfo.getFieldDefinition())
                .executionPath(executionStepInfo.getPath())
                .resolvedValue(createResolvedValue(fetchedValueAnalysis))
                .children(executionResultNodes)
                .build();
        return result;
    }
}
