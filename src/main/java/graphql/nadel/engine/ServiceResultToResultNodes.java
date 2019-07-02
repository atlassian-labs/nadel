package graphql.nadel.engine;

import graphql.GraphQLError;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ExecutionStepInfoFactory;
import graphql.execution.FetchedValue;
import graphql.execution.MergedField;
import graphql.execution.MergedSelectionSet;
import graphql.execution.nextgen.ExecutionStrategyUtil;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.FieldSubSelection;
import graphql.execution.nextgen.ResultNodesCreator;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.NamedResultNode;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;
import graphql.execution.nextgen.result.ResolvedValue;
import graphql.execution.nextgen.result.ResultNodesUtil;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.nadel.ServiceExecutionResult;
import graphql.nadel.util.ErrorUtil;
import graphql.util.FpKit;
import graphql.util.NodeMultiZipper;
import graphql.util.NodeZipper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static graphql.util.FpKit.map;

public class ServiceResultToResultNodes {

    private final ExecutionStepInfoFactory executionStepInfoFactory = new ExecutionStepInfoFactory();
    private final ServiceExecutionResultAnalyzer fetchedValueAnalyzer = new ServiceExecutionResultAnalyzer();
    private final ResultNodesCreator resultNodesCreator = new ResultNodesCreator();
    private final ExecutionStrategyUtil util = new ExecutionStrategyUtil();

    public RootExecutionResultNode resultToResultNode(ExecutionContext executionContext,
                                                      ExecutionStepInfo executionStepInfo,
                                                      List<MergedField> mergedFields,
                                                      ServiceExecutionResult serviceExecutionResult) {

        Map<String, MergedField> subFields = FpKit.getByName(mergedFields, MergedField::getResultKey);
        MergedSelectionSet mergedSelectionSet = MergedSelectionSet.newMergedSelectionSet()
                .subFields(subFields).build();

        FieldSubSelection fieldSubSelectionWithData = FieldSubSelection.newFieldSubSelection().
                executionInfo(executionStepInfo)
                .source(serviceExecutionResult.getData())
                .mergedSelectionSet(mergedSelectionSet)
                .build();

        List<ExecutionResultNode> namedResultNodes = resolveSubSelection(executionContext, fieldSubSelectionWithData);
        List<GraphQLError> errors = ErrorUtil.createGraphQlErrorsFromRawErrors(serviceExecutionResult.getErrors());
        return new RootExecutionResultNode(namedResultNodes, errors);
    }

    private List<ExecutionResultNode> resolveSubSelection(ExecutionContext executionContext, FieldSubSelection fieldSubSelection) {
        return map(fetchSubSelection(executionContext, fieldSubSelection), node -> resolveAllChildNodes(executionContext, node));
    }

    private ExecutionResultNode resolveAllChildNodes(ExecutionContext context, NamedResultNode namedResultNode) {
        NodeMultiZipper<ExecutionResultNode> unresolvedNodes = ResultNodesUtil.getUnresolvedNodes(namedResultNode.getNode());
        List<NodeZipper<ExecutionResultNode>> resolvedNodes = map(unresolvedNodes.getZippers(), unresolvedNode -> resolveNode(context, unresolvedNode));
        return resolvedNodesToResultNode(unresolvedNodes, resolvedNodes);
    }

    private NodeZipper<ExecutionResultNode> resolveNode(ExecutionContext executionContext, NodeZipper<ExecutionResultNode> unresolvedNode) {
        ResolvedValue resolvedValue = unresolvedNode.getCurNode().getResolvedValue();
        ExecutionStepInfo esi = unresolvedNode.getCurNode().getExecutionStepInfo();
        FieldSubSelection fieldSubSelection = util.createFieldSubSelection(executionContext, esi, resolvedValue);
        List<ExecutionResultNode> namedResultNodes = resolveSubSelection(executionContext, fieldSubSelection);
        return unresolvedNode.withNewNode(new ObjectExecutionResultNode(esi, resolvedValue, namedResultNodes));
    }

    private ExecutionResultNode resolvedNodesToResultNode(NodeMultiZipper<ExecutionResultNode> unresolvedNodes,
                                                          List<NodeZipper<ExecutionResultNode>> resolvedNodes) {
        return unresolvedNodes.withReplacedZippers(resolvedNodes).toRootNode();
    }

    private List<NamedResultNode> fetchSubSelection(ExecutionContext executionContext, FieldSubSelection fieldSubSelection) {
        List<FetchedValueAnalysis> fetchedValueAnalysisList = fetchAndAnalyze(executionContext, fieldSubSelection);
        return fetchedValueAnalysisToNodes(fetchedValueAnalysisList);
    }

    private List<FetchedValueAnalysis> fetchAndAnalyze(ExecutionContext context, FieldSubSelection fieldSubSelection) {
        return FpKit.map(fieldSubSelection.getMergedSelectionSet().getSubFieldsList(),
                mergedField -> fetchAndAnalyzeField(context, fieldSubSelection.getSource(), mergedField, fieldSubSelection.getExecutionStepInfo()));
    }

    private FetchedValueAnalysis fetchAndAnalyzeField(ExecutionContext context, Object source, MergedField mergedField,
                                                      ExecutionStepInfo executionStepInfo) {
        ExecutionStepInfo newExecutionStepInfo = executionStepInfoFactory.newExecutionStepInfoForSubField(context, mergedField, executionStepInfo);
        FetchedValue fetchedValue = fetchValue(source, mergedField);
        return analyseValue(context, fetchedValue, newExecutionStepInfo);
    }

    private FetchedValue fetchValue(Object source, MergedField mergedField) {
        if (source == null) {
            return FetchedValue.newFetchedValue()
                    .fetchedValue(null)
                    .rawFetchedValue(null)
                    .errors(Collections.emptyList())
                    .build();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) source;
        Object fetchedValue = map.get(mergedField.getResultKey());
        return FetchedValue.newFetchedValue()
                .fetchedValue(fetchedValue)
                .rawFetchedValue(fetchedValue)
                .errors(Collections.emptyList())
                .build();
    }

    private FetchedValueAnalysis analyseValue(ExecutionContext executionContext, FetchedValue fetchedValue, ExecutionStepInfo executionInfo) {
        return fetchedValueAnalyzer.analyzeFetchedValue(executionContext, fetchedValue, executionInfo);
    }

    private List<NamedResultNode> fetchedValueAnalysisToNodes(List<FetchedValueAnalysis> fetchedValueAnalysisList) {
        return FpKit.map(fetchedValueAnalysisList, fetchedValueAnalysis -> {
            ExecutionResultNode resultNode = resultNodesCreator.createResultNode(fetchedValueAnalysis);
            return new NamedResultNode(fetchedValueAnalysis.getField().getResultKey(), resultNode);
        });
    }


}
